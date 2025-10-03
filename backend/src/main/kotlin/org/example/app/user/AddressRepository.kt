package org.example.app.user

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AddressRepository : JpaRepository<Address, Long> {

    fun findByLine1AndLine2AndCityAndProvinceAndPostalCodeAndCountry(
        line1: String,
        line2: String?,
        city: String,
        province: Province,
        postalCode: String,
        country: String
    ): Address?

    @Query("select a from Address a where a.city = :city and a.province = :province")
    fun findAllByCityAndProvince(
        @Param("city") city: String,
        @Param("province") province: Province
    ): List<Address>
}
