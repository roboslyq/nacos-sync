package com.alibaba.nacossync.extension;

import com.alibaba.nacossync.pojo.model.TaskDO;

import java.util.List;

/**
 * @author roboslyq
 * @version 1.0
 * @date: 2018-15-12 09:11
 */
public interface IRegistryCenter {
    public List<String> doConsumer(TaskDO taskDO);
    public boolean doRegistry(List<String> obj);
}
