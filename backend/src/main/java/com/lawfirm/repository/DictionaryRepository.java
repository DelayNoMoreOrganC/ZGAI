package com.lawfirm.repository;

import com.lawfirm.entity.Dictionary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 字典Repository
 */
@Repository
public interface DictionaryRepository extends JpaRepository<Dictionary, Long> {

    /**
     * 根据字典类型查找字典列表
     */
    List<Dictionary> findByDictTypeOrderBySortOrderAsc(String dictType);

    /**
     * 根据字典类型和字典值查找字典
     */
    Dictionary findByDictTypeAndDictValue(String dictType, String dictValue);
}
