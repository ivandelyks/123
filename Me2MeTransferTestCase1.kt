/**
 * Класс тест-кейс для проверки операции перевода между своими счетами Me2Me
 */
@JiraTestKey("SMCFM-T2")
@DisplayName(Me2MeTransferTestCase1.TEST_CASE_NAME)
class Me2MeTransferTestCase1: CashFlowManagerTestCase() {
    companion object {
        const val TEST_CASE_NAME: String = "Успешный перевод между своими счетами Me2Me"
    }

    private val office = BankOffice.VSP_38_5278_01032
    private val client = Client(office)
    private val account = Account(office)
    private val cardFrom = Card(CardType.MIR_SBERCARD, office, account, client)
    private val cardTo = Card(CardType.VISA_CLASSIC, office, account, client)

    @Test
    @DisplayName(TEST_CASE_NAME)
    override fun run() {
        var response = handle(
            "Получение предложений по распределению средств",
            toRequest(null, "Какие есть предложения по распределению средств?", "domain_agent", "cash_flow_manager_agent", Performative.REQUEST),
            mockConfigs = listOf(
                AppMockConfigs.PFM.GetOperationPredictions.sendRequest()
                    .setResponse(ACLUtils.readJsonResourceFile(
                        "mock/suit/Me2MeTransferTestCase1/get_operation_predictions.json",
                        GetOperationPredictionMockRs::class.java
                    )),
                AppMockConfigs.GigaChat.MakeOffer.sendRequest()
                    .setResponse(
                        ACLUtils.readJsonResourceFile(
                            "mock/suit/Me2MeTransferTestCase1/giga_chat_1.json",
                            MakeOfferResponse::class.java
                        )
                    )
            )
        )

        response = handle(
            "Согласование предложения, переход к выполнению",
            toRequest(response, "Согласен", "agent_human", "cash_flow_manager_agent", Performative.INFORM),
            mockConfigs = listOf(
                AppMockConfigs.GigaChat.MakeOffer.sendRequest()
                    .setResponse(ACLUtils.readJsonResourceFile("mock/suit/Me2MeTransferTestCase1/giga_chat_2.json", MakeOfferResponse::class.java)),
                MockConfigs.PersonProfile.getPersonInfoWithQueryModeAndRegionKind()
                    .setResponseByTestModel(PersonProfileData(client, cardFrom, cardTo))
            )
        )

        handle(
            "Ответ от Workflow адаптера и переход к экрану с результатом",
            toRequest(response, "Перевод между своими счетами успешно выполнен", "agent_workflow", "cash_flow_manager_agent", Performative.INFORM),
        )
    }
}