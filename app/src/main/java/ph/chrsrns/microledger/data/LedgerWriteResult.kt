package ph.chrsrns.microledger.data

import java.io.IOException

/**
 * Outcome of a write operation in [LedgerRepository].
 *
 * - [Success]: the write committed and the repository re-read the file.
 * - [Mismatch]: the file on disk no longer matches the cached contents, so the
 *   write was refused.
 * - [WriteError]: an I/O or permission failure prevented the write.
 * - [RefreshError]: the write committed, but the re-read afterwards failed.
 */
sealed interface LedgerWriteResult {
    data object Success : LedgerWriteResult

    data object Mismatch : LedgerWriteResult

    data class WriteError(
        val e: IOException,
    ) : LedgerWriteResult

    data class RefreshError(
        val e: IOException,
    ) : LedgerWriteResult
}
