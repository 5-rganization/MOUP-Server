package com.moup.server.repository;

import com.moup.server.model.dto.WorkplaceCreateRequest;
import com.moup.server.model.dto.WorkplaceUpdateRequest;
import com.moup.server.model.entity.Workplace;
import org.apache.ibatis.annotations.*;

import java.util.Optional;

public interface WorkplaceRepository {

    /**
     * 근무지를 생성하고, 생성된 근무지의 ID를 반환하는 메서드.
     *
     * @param workplaceCreateRequest 근무지 생성 요청 DTO 객체
     * @return 생성된 근무지의 ID
     */
    @Insert("""
            INSERT INTO workplaces (
                                    owner_id, workplace_name, salary_type, salary_calculation, hourly_rate, fixed_rate, salary_date, salary_day,
                                    has_national_pension, has_health_insurance, has_employment_insurance, has_industrial_accident, has_income_tax, has_night_allowance,
                                    label_color, is_shared, address, latitude, longitude
                                    ) 
            VALUES (
                    #{ownerId}, #{workplaceName}, #{salaryType}, #{salaryCalculation}, #{hourlyRate}, #{fixedRate}, #{salaryDate}, #{salaryDay},
                    #{hasNationalPension}, #{hasHealthInsurance}, #{hasEmploymentInsurance}, #{hasIndustrialAccident}, #{hasIncomeTax}, #{hasNightAllowance},
                    #{labelColor}, #{isShared}, #{address}, #{latitude}, #{longitude}
                    )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    Long create(WorkplaceCreateRequest workplaceCreateRequest);

    /**
     * 근무지 ID를 통해 해당 근무지를 찾고, 그 근무지의 객체를 반환하는 메서드
     *
     * @param id 조회할 근무지의 ID
     * @return 조회된 Workplace 객체, 없으면 Optional.empty
     */
    @Select("SELECT * FROM workplaces WHERE id = #{id}")
    Optional<Workplace> findById(Long id);

    /**
     * id에 해당하는 근무지를 업데이트하는 메서드.
     *
     * @param workplaceUpdateRequest 근무지 수정 요청 DTO 객체
     */
    @Update("""
            UPDATE workplaces SET workplace_name = #{workplaceName}, salary_type = #{salaryType}, salary_calculation = #{salaryCalculation}, 
                                  hourly_rate = #{hourlyRate}, fixed_rate = #{fixedRate}, salary_date = #{salaryDate}, salary_day = #{salaryDay},
                                  has_national_pension = #{hasNationalPension}, has_health_insurance = #{hasHealthInsurance},
                                  has_employment_insurance = #{hasEmploymentInsurance}, has_industrial_accident = #{hasIndustrialAccident}, 
                                  has_income_tax = #{hasIncomeTax}, has_night_allowance = #{hasNightAllowance},
                                  label_color = #{labelColor}, is_shared = #{isShared}, address = #{address}, latitude = #{latitude}, longitude = #{longitude}
            WHERE id = #{id}
            """)
    void update(WorkplaceUpdateRequest workplaceUpdateRequest);

    /**
     * id에 해당하는 근무지를 삭제하는 메서드.
     *
     * @param id 삭제할 근무지의 ID
     */
    @Delete("DELETE FROM workplaces WHERE id = #{id}")
    void deleteById(Long id);
}
