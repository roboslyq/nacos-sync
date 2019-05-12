package com.alibaba.nacossync.extension;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacossync.pojo.model.TaskDO;

/**
 * 实现nacos协议与不同协议之间的转换
 */
public interface NacosProtolExchange<T> {
    public Instance buildNacosInstance(T obj, TaskDO taskDO);
    public T parseNacosInsance(Instance instance, TaskDO taskDO);
}

