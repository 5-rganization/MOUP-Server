package com.moup.server.repository;

import com.moup.server.model.entity.Salary;
import org.apache.ibatis.annotations.*;

import java.util.Optional;

public interface SalaryRepository {
    /**
     * 급여를 생성하고, 생성된 급여의 근무자 ID를 반환하는 메서드.
     *
     * @param salary 생성할 급여 엔티티
     * @return 생성된 급여의 근무자 ID
     */
    @Insert("""
            INSERT INTO salaries (
                                    id, salary_type, salary_calculation, hourly_rate, fixed_rate, salary_date, salary_day,
                                    has_national_pension, has_health_insurance, has_employment_insurance, has_industrial_accident,
                                    has_income_tax, has_night_allowance
                                    ) 
            VALUES (
                    #{id}, #{salaryType}, #{salaryCalculation}, #{hourlyRate}, #{fixedRate}, #{salaryDate}, #{salaryDay},
                    #{hasNationalPension}, #{hasHealthInsurance}, #{hasEmploymentInsurance}, #{hasIndustrialAccident}, #{hasIncomeTax}, #{hasNightAllowance}
                    )
            """)
    Long create(Salary salary);

    /**
     * 근무자 ID를 통해 해당 근무자의 급여를 찾고, 그 급여의 객체를 반환하는 메서드
     *
     * @param workerId 조회할 급여의 근무자 ID
     * @return 조회된 Salary 객체, 없으면 Optional.empty
     */
    @Select("SELECT * FROM salaries WHERE id = #{workerId}")
    Optional<Salary> findByWorkerId(Long workerId);

    /**
     * 근무자 ID에 해당하는 급여 정보를 업데이트하는 메서드.
     *
     * @param workerId 업데이트할 급여의 근무자 ID
     * @param salary   업데이트할 급여 엔티티
     */
    @Update("""
            UPDATE salaries SET salary_type = #{salaryType}, salary_calculation = #{salaryCalculation}, 
                                  hourly_rate = #{hourlyRate}, fixed_rate = #{fixedRate}, salary_date = #{salaryDate}, salary_day = #{salaryDay},
                                  has_national_pension = #{hasNationalPension}, has_health_insurance = #{hasHealthInsurance},
                                  has_employment_insurance = #{hasEmploymentInsurance}, has_industrial_accident = #{hasIndustrialAccident}, 
                                  has_income_tax = #{hasIncomeTax}, has_night_allowance = #{hasNightAllowance}
            WHERE id = #{workerId}
            """)
    void updateByWorkerId(Long workerId, Salary salary);

    /**
     * 근무자 ID에 해당하는 급여를 삭제하는 메서드.
     *
     * @param workerId 삭제할 급여의 근무자 ID
     */
    @Delete("DELETE FROM salaries WHERE id = #{workerId}")
    void deleteByWorkerId(Long workerId);
}
