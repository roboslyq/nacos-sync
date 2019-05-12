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

import com.alibaba.nacossync.constant.ClusterTypeEnum;
import com.alibaba.nacossync.extension.extension.IRegistryCenter;
import com.alibaba.nacossync.extension.extension.annotation.RegistryCenter;
import com.alibaba.nacossync.pojo.model.TaskDO;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Nacos 同步 Zk 数据
 * 
 * @author paderlol
 * @date 2019年01月06日, 15:08:06
 */
@Slf4j
@RegistryCenter(name = ClusterTypeEnum.NACOS)
public class NacosSyncServiceImpl implements IRegistryCenter {
    @Override
    public List<String> doConsumer(TaskDO taskDO) {

        return null;
    }
    @Override
    public boolean doRegistry(List<String> obj) {
        return false;
    }
}
