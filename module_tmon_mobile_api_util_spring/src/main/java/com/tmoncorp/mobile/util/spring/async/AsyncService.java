package com.tmoncorp.mobile.util.spring.async;

import com.tmoncorp.mobile.util.common.async.AsyncExecutor;
import com.tmoncorp.mobile.util.common.async.AsyncTask;
import com.tmoncorp.mobile.util.common.async.AsyncWorker;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

@Service
public class AsyncService implements AsyncWorker {

    private final AsyncExecutor excutor;

    public AsyncService() {
        excutor = new AsyncExecutor();
    }

    @PostConstruct
    public void init() throws Exception {
        excutor.init();
    }

    @PreDestroy
    public void cleanUp() throws Exception {
        excutor.cleanUp();
    }

    public <T> Future<T> submitAsync(Callable<T> call) {

        return excutor.submitAsync(call);
    }

    public <T, K> List<T> processAsyncList(List<K> datas, AsyncTask<K, T> call) {

        return excutor.processAsyncList(datas, call);
    }

    public <K, T> List<T> processAsyncMergeList(List<K> datas, AsyncTask<K, List<T>> call) {

        return excutor.processAsyncMergeList(datas, call);
    }

    @Override public void submitAsync(Runnable run) {
        excutor.submitAsync(run);
    }
}
