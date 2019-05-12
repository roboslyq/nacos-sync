package com.alibaba.nacossync.extension;

import com.alibaba.nacossync.cache.SkyWalkerCacheServices;
import com.alibaba.nacossync.constant.ClusterTypeEnum;
import com.alibaba.nacossync.extension.annotation.RegistryCenter;
import com.alibaba.nacossync.pojo.model.TaskDO;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Hashtable;

/**
 * roboslyq
 */
public class AbstraceSyncService implements SyncService , InitializingBean, ApplicationContextAware {
    private ApplicationContext applicationContext;
    @Autowired
    protected SkyWalkerCacheServices skyWalkerCacheServices;
    private Hashtable<String, IRegistryCenter> registryCenterMap = new Hashtable<String, IRegistryCenter>();

    @Override
    public boolean delete(TaskDO taskDO) {
        return true;
    }

    @Override
    public boolean sync(TaskDO taskDO) {
        ClusterTypeEnum sourceClusterType = this.skyWalkerCacheServices.getClusterType(taskDO.getSourceClusterId());
        ClusterTypeEnum destClusterType = this.skyWalkerCacheServices.getClusterType(taskDO.getDestClusterId());
        IRegistryCenter registryCenterSource = registryCenterMap.get(sourceClusterType);
        IRegistryCenter registryCenterTarget = registryCenterMap.get(destClusterType);
        registryCenterTarget.doRegistry(registryCenterSource.doConsumer(taskDO));
        return false;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        applicationContext.getBeansOfType(IRegistryCenter.class).forEach((key, value) ->{
                    RegistryCenter  registryCenter =  value.getClass().getAnnotation(RegistryCenter.class);
                    //TODO
                    ClusterTypeEnum clusterTypeEnum = registryCenter.name();
                    registryCenterMap.put(clusterTypeEnum.toString(),value);
                }
        );
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
