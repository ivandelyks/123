import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.sber.dcp.dab2c.sdk.model.Performative
import ru.sbrf.dab2c.agent.acl.graph.util.ACLUtils
import ru.sbrf.dev.at.tool.core.action.config.ActionConfig
import ru.sbrf.dev.at.tool.ufs.core.config.UfsActionConfigs
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonSerialize

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
            actionConfigs = mapOf(
                AppActions.RECEIVE_REQUEST.nameAndConfig,
                AppActions.SEND_SYNC_RESPONSE.nameAndConfig,
                AppActions.GET_OPERATION_PREDICTIONS.nameAndConfig,
                AppActions.GIGA_CHAT_MAKE_OFFER.nameAndConfig
            ),
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
            actionConfigs = mapOf(
                AppActions.RECEIVE_REQUEST.nameAndConfig,
                AppActions.SEND_SYNC_RESPONSE.nameAndConfig,
                AppActions.GIGA_CHAT_MAKE_OFFER.nameAndConfig
            ),
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
            actionConfigs = mapOf(
                AppActions.RECEIVE_REQUEST.nameAndConfig,
                AppActions.SEND_SYNC_RESPONSE.nameAndConfig
            )
        )
    }
}

// Константы для действий приложения
private const val RECEIVE_REQUEST_NAME = "Получен входящий запрос"
private const val SEND_SYNC_RESPONSE_NAME = "Отправлен синхронный ответ"
private const val GET_OPERATION_PREDICTIONS_NAME = "Получение предполагаемых операций клиента"
private const val GIGA_CHAT_MAKE_OFFER_NAME = "Формирование списка операций через GigaChat"

private val DEFAULT_APP_ACTION_CONFIG = ActionConfig(
    registrationRequired = true,
    strictOrder = true,
    checkFieldsRequired = true
)

enum class AppActions(
    val actionName: String,
    val actionConfig: ActionConfig
) {
    RECEIVE_REQUEST(
        RECEIVE_REQUEST_NAME,
        ActionConfig(
            registrationRequired = true,
            strictOrder = true,
            checkFieldsRequired = false
        )
    ),

    SEND_SYNC_RESPONSE(
        SEND_SYNC_RESPONSE_NAME,
        UfsActionConfigs.SEND_SYNC_RESPONSE.actionConfig.copy(
            notCheckFields = UfsActionConfigs.SEND_SYNC_RESPONSE.actionConfig.notCheckFields.union(
                setOf(
                    "body",
                    "headers.Date[0]"
                )
            )
        )
    ),

    GET_OPERATION_PREDICTIONS(
        GET_OPERATION_PREDICTIONS_NAME,
        DEFAULT_APP_ACTION_CONFIG
    ),

    GIGA_CHAT_MAKE_OFFER(
        GIGA_CHAT_MAKE_OFFER_NAME,
        DEFAULT_APP_ACTION_CONFIG
    );

    val nameAndConfig = Pair(actionName, actionConfig)
}

// Mock классы для тестирования
@JsonSerialize
class GetOperationPredictionMockRs {
    @JsonProperty("operation_predictions")
    val operationPredictions: List<OperationPrediction> = emptyList()
}

class OperationPrediction {
    var attributes: String = ""
    var type: String = ""
}

class MakeOfferResponse {
    var message: String = ""
    var operationOffers: List<OperationOffer> = emptyList()
    var offerAccepted: Boolean = false
    var exitRequested: Boolean = false
}

class OperationOffer {
    var type: String = ""
    var description: String = ""
    var attributes: Map<String, String> = emptyMap()
}

class PersonProfileData(
    val client: String,
    val cardFrom: String,
    val cardTo: String
)