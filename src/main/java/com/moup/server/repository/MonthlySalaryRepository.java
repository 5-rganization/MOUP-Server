package com.moup.server.repository;

import com.moup.server.model.entity.MonthlySalary;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface MonthlySalaryRepository {

    /// 월별 급여를 생성하는 메서드
    ///
    /// @param monthlySalary 생성할 월별 급여 엔티티
    /// @return 생성된 행의 수
    @Insert("""
            INSERT INTO monthly_salaries (worker_id, salary_month, gross_income, national_pension, health_insurance,
                                          employment_insurance, income_tax, local_income_tax, net_income)
            VALUES (#{workerId}, #{salaryMonth}, #{grossIncome}, #{nationalPension}, #{healthInsurance},
                    #{employmentInsurance}, #{incomeTax}, #{localIncomeTax}, #{netIncome})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    Long create(MonthlySalary monthlySalary);

    /// 근무자 ID에 해당하는 월별 급여를 삭제하는 메서드
    ///
    /// @param workerId 삭제할 월별 급여의 근무자 ID
    @Delete("DELETE FROM monthly_salaries WHERE worker_id = #{workerId}")
    void deleteByWorkerId(Long workerId);
}