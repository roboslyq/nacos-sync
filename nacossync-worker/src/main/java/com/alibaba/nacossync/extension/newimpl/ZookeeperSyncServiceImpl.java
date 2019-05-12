/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.alibaba.nacossync.extension.newimpl;

import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacossync.cache.SkyWalkerCacheServices;
import com.alibaba.nacossync.constant.ClusterTypeEnum;
import com.alibaba.nacossync.constant.MetricsStatisticsType;
import com.alibaba.nacossync.constant.SkyWalkerConstants;
import com.alibaba.nacossync.extension.IRegistryCenter;
import com.alibaba.nacossync.extension.NacosProtolExchange;
import com.alibaba.nacossync.extension.SyncService;
import com.alibaba.nacossync.extension.annotation.NacosSyncService;
import com.alibaba.nacossync.extension.annotation.RegistryCenter;
import com.alibaba.nacossync.extension.holder.NacosServerHolder;
import com.alibaba.nacossync.extension.holder.ZookeeperServerHolder;
import com.alibaba.nacossync.monitor.MetricsManager;
import com.alibaba.nacossync.pojo.model.TaskDO;
import com.google.common.base.Joiner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.utils.CloseableUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import static com.alibaba.nacossync.util.DubboConstants.*;
import static com.alibaba.nacossync.util.StringUtils.*;

/**
 * @author paderlol
 * @version 1.0
 * @date: 2018-12-24 21:33
 */
@Slf4j
@RegistryCenter(name = ClusterTypeEnum.ZK)
public class ZookeeperSyncServiceImpl implements IRegistryCenter, NacosProtolExchange<String> {
    @Autowired
    private MetricsManager metricsManager;
    /**
     * Listener cache of Zookeeper  format taskId -> PathChildrenCache instance
     */
    private Map<String, PathChildrenCache> pathChildrenCacheMap = new ConcurrentHashMap<>();
    /**
     * service name cache
     */
    private Map<String, String> nacosServiceNameMap = new ConcurrentHashMap<>();

    private final ZookeeperServerHolder zookeeperServerHolder;

    private final NacosServerHolder nacosServerHolder;

    private final SkyWalkerCacheServices skyWalkerCacheServices;

    @Autowired
    public ZookeeperSyncServiceImpl(ZookeeperServerHolder zookeeperServerHolder,
                                    NacosServerHolder nacosServerHolder, SkyWalkerCacheServices skyWalkerCacheServices) {
        this.zookeeperServerHolder = zookeeperServerHolder;
        this.nacosServerHolder = nacosServerHolder;
        this.skyWalkerCacheServices = skyWalkerCacheServices;
    }
    @Override
    public List<String> doConsumer(TaskDO taskDO) {
        List<String> pathList = new ArrayList<>();
        try {
            if (pathChildrenCacheMap.containsKey(taskDO.getTaskId())) {
                return pathList;
            }
            PathChildrenCache pathChildrenCache = getPathCache(taskDO);
            NamingService destNamingService = nacosServerHolder
                    .get(taskDO.getDestClusterId(), null);
            List<ChildData> currentData = pathChildrenCache.getCurrentData();
            for (ChildData childData : currentData) {
                String path = childData.getPath();
                Map<String, String> queryParam = parseQueryString(childData.getPath());
                if (isMatch(taskDO, queryParam) && needSync(queryParam)) {
                    pathList.add(path);
                }
            }
            Objects.requireNonNull(pathChildrenCache).getListenable()
                    .addListener((client, event) -> {
                        try {

                            String path = event.getData().getPath();
                            pathList.add(path);
                        } catch (Exception e) {
                            log.error("event process from zookeeper to nacos was failed, taskId:{}",
                                    taskDO.getTaskId(), e);
                            metricsManager.recordError(MetricsStatisticsType.SYNC_ERROR);
                        }

                    });
        } catch (Exception e) {
            log.error("sync task from zookeeper to nacos was failed, taskId:{}", taskDO.getTaskId(),
                    e);
            metricsManager.recordError(MetricsStatisticsType.SYNC_ERROR);
            return null;
        }
        return pathList;
    }
    /**
     * Determines that the current instance data is from another source cluster
     */
    boolean needSync(Map<String, String> sourceMetaData) {
        return StringUtils.isBlank(sourceMetaData.get(SkyWalkerConstants.SOURCE_CLUSTERID_KEY));
    }
    @Override
    public boolean doRegistry(List<String> obj) {
        return false;
    }
    /**
     * fetch the Path cache when the task sync
     */
    protected PathChildrenCache getPathCache(TaskDO taskDO) {
        return pathChildrenCacheMap.computeIfAbsent(taskDO.getTaskId(), (key) -> {
            try {
                PathChildrenCache pathChildrenCache =
                        new PathChildrenCache(
                                zookeeperServerHolder.get(taskDO.getSourceClusterId(), ""),
                                convertDubboProvidersPath(taskDO.getServiceName()), false);
                pathChildrenCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
                return pathChildrenCache;
            } catch (Exception e) {
                log.error("zookeeper path children cache start failed, taskId:{}",
                        taskDO.getTaskId(), e);
                return null;
            }
        });

    }



    /**
     * The instance information that needs to be synchronized is matched based on the dubbo version
     * and the grouping name
     */
    protected boolean isMatch(TaskDO taskDO, Map<String, String> queryParam) {
        Predicate<TaskDO> isVersionEq =
                (task) -> StringUtils.isBlank(taskDO.getVersion()) || StringUtils
                        .equals(task.getVersion(), queryParam.get(VERSION_KEY));
        Predicate<TaskDO> isGroupEq =
                (task) -> StringUtils.isBlank(taskDO.getGroupName()) || StringUtils
                        .equals(task.getGroupName(), queryParam.get(GROUP_KEY));
        return isVersionEq.and(isGroupEq).test(taskDO);
    }

    /**
     * create Nacos service instance
     *
     * @param queryParam dubbo metadata
     * @param ipAndPortMap dubbo ip and address
     */
    protected Instance buildSyncInstance(Map<String, String> queryParam,
            Map<String, String> ipAndPortMap,
            TaskDO taskDO) {
        Instance temp = new Instance();
        temp.setIp(ipAndPortMap.get(INSTANCE_IP_KEY));
        temp.setPort(Integer.parseInt(ipAndPortMap.get(INSTANCE_PORT_KEY)));
        temp.setServiceName(getServiceNameFromCache(taskDO.getTaskId(), queryParam));
        temp.setWeight(Double.valueOf(
                queryParam.get(WEIGHT_KEY) == null ? "1.0" : queryParam.get(WEIGHT_KEY)));
        temp.setHealthy(true);

        Map<String, String> metaData = new HashMap<>(queryParam);
        metaData.put(PROTOCOL_KEY, ipAndPortMap.get(PROTOCOL_KEY));
        metaData.put(SkyWalkerConstants.DEST_CLUSTERID_KEY, taskDO.getDestClusterId());
        metaData.put(SkyWalkerConstants.SYNC_SOURCE_KEY,
                skyWalkerCacheServices.getClusterType(taskDO.getSourceClusterId()).getCode());
        metaData.put(SkyWalkerConstants.SOURCE_CLUSTERID_KEY, taskDO.getSourceClusterId());
        temp.setMetadata(metaData);
        return temp;
    }

    /**
     * cteate Dubbo service name
     *
     * @param taskId task id
     * @param queryParam dubbo metadata
     */
    protected String getServiceNameFromCache(String taskId, Map<String, String> queryParam) {
        return nacosServiceNameMap
                .computeIfAbsent(taskId, (key) -> Joiner.on(":").skipNulls().join(CATALOG_KEY,
                        queryParam.get(INTERFACE_KEY), queryParam.get(VERSION_KEY),
                        queryParam.get(GROUP_KEY)));
    }

    @Override
    public Instance buildNacosInstance(String path, TaskDO taskDO) {
        Instance instance = null;
        Map<String, String> queryParam = parseQueryString(path);
        if (isMatch(taskDO, queryParam) && needSync(queryParam)) {
            Map<String, String> ipAndPortParam = parseIpAndPortString(path);
            instance = buildSyncInstance(queryParam, ipAndPortParam, taskDO);
        }
        return instance;
    }

    @Override
    public String parseNacosInsance(Instance instance, TaskDO taskDO) {
        return null;
    }
}
