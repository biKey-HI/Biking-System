package org.example.app

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.example.app.bmscoreandstationcontrol.api.DockingStationResponse
import org.example.app.bmscoreandstationcontrol.persistence.DockingStationEntity
import org.example.app.bmscoreandstationcontrol.persistence.DockingStationRepository
import org.example.app.user.AddressRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

@Component
class DockingStationBootstrap(
    private val dockingStationRepository: DockingStationRepository,
    private val addressRepository: AddressRepository
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        dockingStationRepository.deleteAll()

        val resource = ClassPathResource("DockingStationConfiguration.json")
        val mapper = jacksonObjectMapper()
        val stations: List<DockingStationResponse> = mapper.readValue(resource.inputStream)

        stations.forEach { resp ->
            val domainStation = resp.toDomain()
            val savedAddress = addressRepository.findByLine1AndLine2AndCityAndProvinceAndPostalCodeAndCountry(
                domainStation.address.line1,
                domainStation.address.line2,
                domainStation.address.city,
                domainStation.address.province,
                domainStation.address.postalCode,
                domainStation.address.country
            ) ?: addressRepository.save(domainStation.address)

            val entity = DockingStationEntity(domainStation.copy(address = savedAddress))
            dockingStationRepository.save(entity)
        }

        println("Bootstrapped ${stations.size} docking stations.")
    }
}