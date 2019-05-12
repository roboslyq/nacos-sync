package com.alibaba.nacossync.extension;

import com.alibaba.nacossync.pojo.model.TaskDO;

import java.util.List;

/**
 * 所有注册中心抽象
 */
public interface IRegistryCenter {
    public List<String> doConsumer(TaskDO taskDO);
    public boolean doRegistry(List<String> obj);
}
