package com.alibaba.nacossync.extension;

import com.alibaba.nacossync.pojo.model.TaskDO;

import java.util.Hashtable;
/**
 * @author roboslyq
 * @version 1.0
 * @date: 2018-15-12 09:11
 */
public class AbstraceSyncService implements SyncService {
    private Hashtable<String, SyncService> syncServiceSourceMap = new Hashtable<String, SyncService>();
    private Hashtable<String, SyncService> syncServiceTargeMap = new Hashtable<String, SyncService>();

    @Override
    public boolean delete(TaskDO taskDO) {
        return false;
    }

    @Override
    public boolean sync(TaskDO taskDO) {
        return false;
    }
}
