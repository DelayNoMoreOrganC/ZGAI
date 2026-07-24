package com.lawfirm.service;

import com.lawfirm.entity.CaseTimeline;
import com.lawfirm.entity.User;
import com.lawfirm.repository.CaseTimelineRepository;
import com.lawfirm.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CaseTimelineServiceTest {
    @Test
    void timelineDisplaysRealNameAndLoginAccount() {
        CaseTimelineRepository timelineRepository = mock(CaseTimelineRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        CaseTimelineService service = new CaseTimelineService(timelineRepository, userRepository);
        CaseTimeline human = timeline(1L, 9L);
        CaseTimeline system = timeline(2L, 0L);
        User user = new User();
        user.setId(9L);
        user.setRealName("张律师");
        user.setUsername("zhanglvshi");
        when(timelineRepository.findByCaseIdAndDeletedFalseOrderByCreatedAtDesc(8L))
                .thenReturn(List.of(human, system));
        when(userRepository.findAllById(List.of(9L))).thenReturn(List.of(user));

        List<CaseTimeline> result = service.getByCaseId(8L);

        assertEquals("张律师（zhanglvshi）", result.get(0).getOperatorName());
        assertEquals("系统", result.get(1).getOperatorName());
    }

    private CaseTimeline timeline(Long id, Long operatorId) {
        CaseTimeline item = new CaseTimeline();
        item.setId(id);
        item.setCaseId(8L);
        item.setOperatorId(operatorId);
        item.setActionType("TEST");
        item.setActionContent("修改了案件");
        item.setDeleted(false);
        return item;
    }
}
