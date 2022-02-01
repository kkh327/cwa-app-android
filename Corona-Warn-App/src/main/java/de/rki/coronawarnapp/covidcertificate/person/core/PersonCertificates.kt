package de.rki.coronawarnapp.covidcertificate.person.core

import de.rki.coronawarnapp.ccl.dccwalletinfo.model.DccWalletInfo
import de.rki.coronawarnapp.ccl.ui.text.format
import de.rki.coronawarnapp.covidcertificate.common.certificate.CertificatePersonIdentifier
import de.rki.coronawarnapp.covidcertificate.common.certificate.CwaCovidCertificate
import de.rki.coronawarnapp.util.HashExtensions.toSHA256

data class PersonCertificates(
    val certificates: List<CwaCovidCertificate>,
    val isCwaUser: Boolean = false,
    val badgeCount: Int = 0,
    val dccWalletInfo: DccWalletInfo? = null
) {
    val personIdentifier: CertificatePersonIdentifier?
        get() = certificates.firstOrNull()?.personIdentifier

    // PersonDetails
    val highestPriorityCertificate: CwaCovidCertificate? by lazy {
        certificates.firstOrNull { certificate ->
            certificate.qrCodeHash == dccWalletInfo?.mostRelevantCertificate
                ?.certificateRef?.barcodeData?.toSHA256()
        } ?: certificates.findFallbackDcc()
    }

    // PersonOverview
    val certificatesForOverviewScreen: List<VerificationCertificate> by lazy {
        dccWalletInfo?.verification?.certificates.orEmpty().mapNotNull { certRef ->
            certificates.firstOrNull { it.qrCodeHash == certRef.certificateRef.barcodeData.toSHA256() }?.let {
                VerificationCertificate(
                    certificate = it,
                    buttonText = certRef.buttonText.format()
                )
            }
        }.take(2).ifEmpty {
            when (val cert = certificates.findFallbackDcc()) {
                null -> emptyList()
                else -> listOf(VerificationCertificate(cert))
            }
        }
    }

    // TODO Obsolete, remove in cleanup PR
    val admissionState: AdmissionState? get() = certificates.determineAdmissionState()

    sealed class AdmissionState(val primaryCertificate: CwaCovidCertificate) {
        data class TwoGPlusPCR(val twoGCertificate: CwaCovidCertificate, val testCertificate: CwaCovidCertificate) :
            AdmissionState(twoGCertificate)

        data class TwoGPlusRAT(val twoGCertificate: CwaCovidCertificate, val testCertificate: CwaCovidCertificate) :
            AdmissionState(twoGCertificate)

        data class TwoG(val twoGCertificate: CwaCovidCertificate) : AdmissionState(twoGCertificate)

        data class ThreeGWithPCR(val testCertificate: CwaCovidCertificate) : AdmissionState(testCertificate)
        data class ThreeGWithRAT(val testCertificate: CwaCovidCertificate) : AdmissionState(testCertificate)

        data class Other(val otherCertificate: CwaCovidCertificate) : AdmissionState(otherCertificate)
    }
}

data class VerificationCertificate(
    val certificate: CwaCovidCertificate,
    val buttonText: String = ""
)
