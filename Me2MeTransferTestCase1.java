package ru.sbrf.dab2c.agent.cfm.autotest.suit.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.sber.qa.allure.system.System;
import ru.sbrf.dev.at.tool.core.mock.config.MockConfig;
import ru.sber.dcp.dab2c.sdk.model.Performative;
import ru.sbrf.dab2c.agent.acl.graph.util.ACLUtils;
import ru.sbrf.dab2c.agent.cfm.autotest.suit.rest.CashFlowManagerTestCase;

import java.util.List;

/**
 * Класс тест-кейс для проверки операции перевода между своими счетами Me2Me
 */
@System("CI10097750")
@DisplayName(Me2MeTransferTestCase1.TEST_CASE_NAME)
class Me2MeTransferTestCase1 extends CashFlowManagerTestCase {
    static final String TEST_CASE_NAME = "Успешный перевод между своими счетами Me2Me";

    private final String office = "VSP_38_5278_01032";
    private final String client = "Client(" + office + ")";
    private final String account = "Account(" + office + ")";
    private final String cardFrom = "Card(CardType.MIR_SBERCARD, " + office + ", " + account + ", " + client + ")";
    private final String cardTo = "Card(CardType.VISA_CLASSIC, " + office + ", " + account + ", " + client + ")";

    @Test
    @DisplayName(TEST_CASE_NAME)
    @Override
    public void run() {
        var response = handle(
            "Получение предложений по распределению средств",
            toRequest(null, "Какие есть предложения по распределению средств?", "domain_agent", "cash_flow_manager_agent", Performative.REQUEST),
            mockConfigs = List.of(
                AppMockConfigs.PFM.GetOperationPredictions.sendRequest()
                    .setResponse(ACLUtils.readJsonResourceFile(
                        "mock/suit/Me2MeTransferTestCase1/get_operation_predictions.json",
                        GetOperationPredictionMockRs.class
                    )),
                AppMockConfigs.GigaChat.MakeOffer.sendRequest()
                    .setResponse(
                        ACLUtils.readJsonResourceFile(
                            "mock/suit/Me2MeTransferTestCase1/giga_chat_1.json",
                            MakeOfferResponse.class
                        )
                    )
            )
        );

        response = handle(
            "Согласование предложения, переход к выполнению",
            toRequest(response, "Согласен", "agent_human", "cash_flow_manager_agent", Performative.INFORM),
            mockConfigs = List.of(
                AppMockConfigs.GigaChat.MakeOffer.sendRequest()
                    .setResponse(ACLUtils.readJsonResourceFile("mock/suit/Me2MeTransferTestCase1/giga_chat_2.json", MakeOfferResponse.class)),
                MockConfigs.PersonProfile.getPersonInfoWithQueryModeAndRegionKind()
                    .setResponseByTestModel(PersonProfileData(client, cardFrom, cardTo))
            )
        );

        handle(
            "Ответ от Workflow адаптера и переход к экрану с результатом",
            toRequest(response, "Перевод между своими счетами успешно выполнен", "agent_workflow", "cash_flow_manager_agent", Performative.INFORM),
        );
    }
}

/**
 * Mock класс для ответа с предсказаниями операций
 */
@JsonSerialize
class GetOperationPredictionMockRs {
    @JsonProperty("operation_predictions")
    private List<OperationPrediction> operationPredictions = List.of();

    public List<OperationPrediction> getOperationPredictions() {
        return operationPredictions;
    }

    public void setOperationPredictions(List<OperationPrediction> operationPredictions) {
        this.operationPredictions = operationPredictions;
    }
}

/**
 * Класс для предсказания операции
 */
class OperationPrediction {
    private String attributes;
    private String type;

    public String getAttributes() {
        return attributes;
    }

    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}

/**
 * Класс для ответа от GigaChat
 */
class MakeOfferResponse {
    private String message;
    private List<OperationOffer> operationOffers;
    private boolean offerAccepted;
    private boolean exitRequested;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<OperationOffer> getOperationOffers() {
        return operationOffers;
    }

    public void setOperationOffers(List<OperationOffer> operationOffers) {
        this.operationOffers = operationOffers;
    }

    public boolean isOfferAccepted() {
        return offerAccepted;
    }

    public void setOfferAccepted(boolean offerAccepted) {
        this.offerAccepted = offerAccepted;
    }

    public boolean isExitRequested() {
        return exitRequested;
    }

    public void setExitRequested(boolean exitRequested) {
        this.exitRequested = exitRequested;
    }
}

/**
 * Класс для предложения операции
 */
class OperationOffer {
    private String type;
    private String description;
    private java.util.Map<String, String> attributes;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public java.util.Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(java.util.Map<String, String> attributes) {
        this.attributes = attributes;
    }
}

/**
 * Класс для данных профиля пользователя
 */
class PersonProfileData {
    private String client;
    private String cardFrom;
    private String cardTo;

    public PersonProfileData(String client, String cardFrom, String cardTo) {
        this.client = client;
        this.cardFrom = cardFrom;
        this.cardTo = cardTo;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getCardFrom() {
        return cardFrom;
    }

    public void setCardFrom(String cardFrom) {
        this.cardFrom = cardFrom;
    }

    public String getCardTo() {
        return cardTo;
    }

    public void setCardTo(String cardTo) {
        this.cardTo = cardTo;
    }
}