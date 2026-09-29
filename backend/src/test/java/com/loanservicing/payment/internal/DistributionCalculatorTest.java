package com.loanservicing.payment.internal;

import com.loanservicing.loan.FundingShare;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DistributionCalculatorTest {

    @Test
    void splitsByShareAndPaysLendersTheirOwnRate() {
        var shares = List.of(
                new FundingShare(1L, new BigDecimal("180000"), new BigDecimal("0.6"), new BigDecimal("10"), new BigDecimal("12")),
                new FundingShare(2L, new BigDecimal("120000"), new BigDecimal("0.4"), new BigDecimal("10"), new BigDecimal("12")));

        var splits = DistributionCalculator.split(new BigDecimal("1000.00"), new BigDecimal("500.00"), shares);

        assertThat(splits).hasSize(2);
        assertThat(splits.get(0).principal()).isEqualByComparingTo("600.00");
        assertThat(splits.get(0).interest()).isEqualByComparingTo("250.00");   // 500 x 0.6 x 10/12
        assertThat(splits.get(1).principal()).isEqualByComparingTo("400.00");
        assertThat(splits.get(1).interest()).isEqualByComparingTo("166.67");   // 500 x 0.4 x 10/12
        // servicer keeps 500 - 250 - 166.67 = 83.33
    }

    @Test
    void roundingRemainderGoesToTheLastLender() {
        var third = new BigDecimal("0.3333333333");
        var shares = List.of(
                new FundingShare(1L, BigDecimal.ONE, third, BigDecimal.TEN, BigDecimal.TEN),
                new FundingShare(2L, BigDecimal.ONE, third, BigDecimal.TEN, BigDecimal.TEN),
                new FundingShare(3L, BigDecimal.ONE, third, BigDecimal.TEN, BigDecimal.TEN));

        var splits = DistributionCalculator.split(new BigDecimal("100.00"), BigDecimal.ZERO, shares);

        BigDecimal total = splits.stream().map(DistributionCalculator.LenderSplit::principal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo("100.00");
        assertThat(splits.get(2).principal()).isEqualByComparingTo("33.34");
    }
}
