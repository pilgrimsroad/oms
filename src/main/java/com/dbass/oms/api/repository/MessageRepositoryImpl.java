package com.dbass.oms.api.repository;

import com.dbass.oms.api.entity.MessageLog;
import com.dbass.oms.api.entity.QMessageLog;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MessageRepositoryImpl implements MessageRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<MessageLog> findByFilters(
            String start,
            String end,
            Integer status,
            Integer msgType,
            String recipient,
            Pageable pageable
    ) {
        QMessageLog m = QMessageLog.messageLog;

        BooleanBuilder where = new BooleanBuilder();
        where.and(m.submitTime.between(start, end));
        if (status != null)    where.and(m.status.eq(status));
        if (msgType != null)   where.and(m.msgType.eq(msgType));
        if (recipient != null) where.and(m.rcptData.contains(recipient));

        List<MessageLog> content = queryFactory
                .selectFrom(m)
                .where(where)
                .orderBy(m.submitTime.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long totalCount = queryFactory
                .select(m.count())
                .from(m)
                .where(where)
                .fetchOne();
        long total = totalCount != null ? totalCount : 0L;

        return new PageImpl<>(content, pageable, total);
    }
}
