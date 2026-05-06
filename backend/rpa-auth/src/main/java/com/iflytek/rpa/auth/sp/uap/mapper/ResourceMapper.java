package com.iflytek.rpa.auth.sp.uap.mapper;

import com.iflytek.rpa.auth.core.entity.Resource;
import com.iflytek.sec.uap.client.core.dto.resource.UapResource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Resource기기
 * 사용를UAP클라이언트의UapResource변환로core패키지아래의Resource
 *
 * @author xqcao2
 */
@Component
public class ResourceMapper {

    /**
     * 를UAP클라이언트의UapResource변환로Resource
     *
     * @param uapResource UAP클라이언트의UapResource
     * @return core패키지아래의Resource
     */
    public Resource fromUapResource(UapResource uapResource) {
        if (uapResource == null) {
            return null;
        }

        Resource resource = new Resource();
        // 사용BeanUtils복사속성
        BeanUtils.copyProperties(uapResource, resource);

        return resource;
    }

    /**
     * 량를UAP클라이언트의UapResource목록변환로Resource목록
     *
     * @param uapResources UAP클라이언트의UapResource목록
     * @return core패키지아래의Resource목록
     */
    public List<Resource> fromUapResources(List<UapResource> uapResources) {
        if (uapResources == null || uapResources.isEmpty()) {
            return Collections.emptyList();
        }

        return uapResources.stream()
                .map(this::fromUapResource)
                .filter(resource -> resource != null)
                .collect(Collectors.toList());
    }

    /**
     * 를core패키지아래의Resource변환로UAP클라이언트의UapResource
     *
     * @param resource core패키지아래의Resource
     * @return UAP클라이언트의UapResource
     */
    public UapResource toUapResource(Resource resource) {
        if (resource == null) {
            return null;
        }

        UapResource uapResource = new UapResource();
        // 사용BeanUtils복사속성
        BeanUtils.copyProperties(resource, uapResource);

        return uapResource;
    }

    /**
     * 량를core패키지아래의Resource목록변환로UAP클라이언트의UapResource목록
     *
     * @param resources core패키지아래의Resource목록
     * @return UAP클라이언트의UapResource목록
     */
    public List<UapResource> toUapResources(List<Resource> resources) {
        if (resources == null || resources.isEmpty()) {
            return Collections.emptyList();
        }

        return resources.stream()
                .map(this::toUapResource)
                .filter(uapResource -> uapResource != null)
                .collect(Collectors.toList());
    }
}