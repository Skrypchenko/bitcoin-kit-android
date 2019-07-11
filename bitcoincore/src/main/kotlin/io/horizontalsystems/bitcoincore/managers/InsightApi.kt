package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.core.IInitialSyncApi
import io.horizontalsystems.bitcoincore.utils.HSLogger

class InsightApi(host: String) : IInitialSyncApi {
    private val apiManager = ApiManager(host)
    private val logger = HSLogger("InsightApi")

    override fun getTransactions(addresses: List<String>): List<TransactionItem> {
        logger.i("Request transactions for %d addresses: [%s, ...]", addresses.size, addresses.first())

        val transactions = mutableListOf<TransactionItem>()

        fetchTransactions(addresses, transactions, 0, 50)

        return transactions
    }

    private fun fetchTransactions(addrs: List<String>, txs: MutableList<TransactionItem>, from: Int, to: Int) {
        val joinedAddresses = addrs.joinToString(",")
        val json = apiManager.getJson("addrs/$joinedAddresses/txs?from=$from&to=$to")

        val totalItems = json["totalItems"].asInt()
        val receivedTo = json["to"].asInt()

        val items = json["items"].asArray()
        for (item in items) {
            val tx = item.asObject()

            val blockHash = tx["blockhash"] ?: continue
            val blockheight = tx["blockheight"] ?: continue
            val outputs = mutableListOf<TransactionOutputItem>()

            for (outputItem in tx["vout"].asArray()) {
                val outputJson = outputItem.asObject()

                val scriptJson = (outputJson["scriptPubKey"] ?: continue).asObject()
                val script = (scriptJson["hex"] ?: continue).asString()
                val addresses = (scriptJson["addresses"] ?: continue).asArray()

                outputs.add(TransactionOutputItem(script, addresses[0].asString()))
            }

            txs.add(TransactionItem(blockHash.asString(), blockheight.asInt(), outputs))
        }

        if (totalItems > to) {
            fetchTransactions(addrs, txs, receivedTo, receivedTo + 50)
        }
    }
}
