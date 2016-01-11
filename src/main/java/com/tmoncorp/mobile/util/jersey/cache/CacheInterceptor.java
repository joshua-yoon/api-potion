package com.tmoncorp.mobile.util.jersey.cache;

import com.tmoncorp.mobile.util.common.cache.*;
import com.tmoncorp.mobile.util.common.security.SecurityUtils;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;


public class CacheInterceptor implements MethodInterceptor ,EtagRegister {
	private static final Logger LOG = LoggerFactory.getLogger(CacheInterceptor.class);

	private static final String KEY_SEPERATOR = ":";
	private static final int LONG_KEY = 100;

	private final CacheInterceptorService ciService;
	private final MemoryCache memoryCache;

	public CacheInterceptor(CacheInterceptorService service) {
		ciService = service;
		memoryCache = new MemoryCache();
		service.getCacheRepo().setEtagRegister(this);
		memoryCache.setEtagRegister(this);
	}

	private String makeKeyName(MethodInvocation invo) {
		Method method = invo.getMethod();

		StringBuilder cb = new StringBuilder();
		cb.append(method.getDeclaringClass().getSimpleName());
		cb.append(KEY_SEPERATOR);
		cb.append(method.getName());

		StringBuilder cp = new StringBuilder();
		for (Object param : invo.getArguments()) {
			cp.append(KEY_SEPERATOR);
			cp.append(param);
		}
		if (cp.length() < LONG_KEY)
			cb.append(cp);
		else {
			cb.append(SecurityUtils.getHash(cp.toString(),SecurityUtils.MD5));
		}
		return cb.toString();
	}

	private Object getMemcache(Cache cacheInfo, MethodInvocation mi) throws Throwable {
		CacheRepository cacheRepo = ciService.getCacheRepo();
		if (cacheRepo == null)
			return mi.proceed();

		String keyName = makeKeyName(mi);
		Object response;
		response = cacheRepo.get(keyName);
		if (response != null) {
			setEtagCache(keyName);
			return response;
		}

		int expire = cacheInfo.expiration();
		response = mi.proceed();
		cacheRepo.set(keyName, response, expire);
		generateEtag(keyName,cacheInfo,mi);
		return response;
	}

	private Object getMemoryCache(Cache cacheInfo, MethodInvocation mi) throws Throwable {
		String keyName = makeKeyName(mi);
		Object cache = memoryCache.get(keyName,cacheInfo,mi);
		if (cache != null) {
			setEtagCache(keyName);
			return cache;
		}
		cache = mi.proceed();
		memoryCache.set(keyName, cache, cacheInfo);
		generateEtag(keyName,cacheInfo,mi);
		return cache;
	}

	private Object getCompositeCache(Cache cacheInfo, MethodInvocation mi) throws Throwable {
		String keyName = makeKeyName(mi);
		Object cache = memoryCache.get(keyName,cacheInfo,mi);
		if (cache != null) {
			setEtagCache(keyName);
			return cache;
		}
		cache = getMemcache(cacheInfo, mi);
		memoryCache.set(keyName, cache, cacheInfo);
		generateEtag(keyName,cacheInfo,mi);
		return cache;
	}

	private void generateEtag(String keyName, Cache cacheInfo, MethodInvocation mi){
		if (cacheInfo.browserCache() == BrowserCache.ETAG){
			String etag= SecurityUtils.getSHA1String(keyName + cacheInfo.expiration());
			ciService.getCacheRepo().set("e:"+keyName,etag,cacheInfo.expiration());
			ciService.getCacheRepo().set(etag,"e:"+keyName,cacheInfo.expiration());
			setEtag(etag);
		}
	}

	private void setEtagCache(String keyName){
		Object etag=ciService.getCacheRepo().get("e:"+keyName);
		if (etag != null)
			setEtag((String)etag);
	}

	public void setEtag(String etag){
		ciService.getHttpServletRequest().setAttribute("etag",etag);
	}


	@Override
	public Object invoke(MethodInvocation mi) throws Throwable {

		CacheRepository cacheRepo = ciService.getCacheRepo();
		if (cacheRepo == null || cacheRepo.getMode() ==CacheMode.OFF)
			return mi.proceed();

		Method method = mi.getMethod();
		Cache cacheInfo = method.getAnnotation(Cache.class);
		if (cacheInfo.type() == CacheType.MEMORY) {
			return getMemoryCache(cacheInfo, mi);
		} else if (cacheInfo.type() == CacheType.COMPOSITE) {
			return getCompositeCache(cacheInfo, mi);
		}
		return getMemcache(cacheInfo, mi);
	}

}
