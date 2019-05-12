package com.alibaba.nacossync.extension;

import com.alibaba.nacossync.pojo.model.TaskDO;

import java.util.List;

/**
 * 注册中心抽象，包括查询和注册，其中注册分3种：update add delete
 * @author roboslyq
 * @version 1.0
 * @date: 2018-15-12 09:11
 */
public interface IRegistryCenter {
    public List<String> doConsumer(TaskDO taskDO);
    public boolean doRegistry(List<String> obj);
}
