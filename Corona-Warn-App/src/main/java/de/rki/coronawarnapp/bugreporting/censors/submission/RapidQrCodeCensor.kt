package de.rki.coronawarnapp.bugreporting.censors.submission

import dagger.Reusable
import de.rki.coronawarnapp.bugreporting.censors.BugCensor
import de.rki.coronawarnapp.bugreporting.censors.BugCensor.CensorContainer
import de.rki.coronawarnapp.bugreporting.censors.BugCensor.Companion.withValidName
import de.rki.coronawarnapp.coronatest.qrcode.RapidAntigenHash
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@Reusable
class RapidQrCodeCensor @Inject constructor() : BugCensor {

    private val dayOfBirthFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override suspend fun checkLog(message: String): CensorContainer? {

        if (dataToCensor.isEmpty()) return null

        var newMessage = CensorContainer(message)

        dataToCensor.forEach { data ->
            with(data) {
                newMessage = newMessage.censor(rawString, createReplacement(input = "ScannedRawString"))

                newMessage = newMessage.censor(hash, PLACEHOLDER + hash.takeLast(4))

                withValidName(firstName) { firstName ->
                    newMessage = newMessage.censor(firstName, createReplacement(input = "FirstName"))
                    newMessage = newMessage.censor(firstName.uppercase(), createReplacement(input = "FirstName"))
                }

                withValidName(lastName) { lastName ->
                    newMessage = newMessage.censor(lastName, createReplacement(input = "LastName"))
                    newMessage = newMessage.censor(lastName.uppercase(), createReplacement(input = "FirstName"))
                }

                val dateOfBirthString = dateOfBirth?.format(dayOfBirthFormatter) ?: return@with

                newMessage = newMessage.censor(dateOfBirthString, createReplacement(input = "DateOfBirth"))
            }
        }

        return newMessage.nullIfEmpty()
    }

    private fun createReplacement(input: String): String = "$PLACEHOLDER_TYPE/$input"

    companion object {
        val dataToCensor = mutableSetOf<CensorData>()

        private const val PLACEHOLDER = "SHA256HASH-ENDING-WITH-"
        private const val PLACEHOLDER_TYPE = "RapidTest"
    }

    data class CensorData(
        val rawString: String,
        val hash: RapidAntigenHash,
        val firstName: String?,
        val lastName: String?,
        val dateOfBirth: LocalDate?
    )
}
