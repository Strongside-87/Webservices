package dk.dtu.pay.tokenmanagement;

import dk.dtu.pay.utils.messaging.Event;
import dk.dtu.pay.utils.messaging.MessageQueue;
import dk.dtu.pay.utils.messaging.QueueNames;
import dk.dtu.pay.utils.models.TokenRequest;
import dk.dtu.pay.utils.models.User;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Muhammad
 */

public class Service {

    private final static int MINIMUM_NUM_OF_TOKENS = 1;
    private final static int MAXIMUM_NUM_OF_TOKENS = 6;
    private final MessageQueue messageQueue;
    private Repository repository = new Repository();
    private Logger logger = Logger.getLogger(Service.class.getName());

    public Service(MessageQueue messageQueue) {
        this.messageQueue = messageQueue;
        addCleanAccountManagementRequestedSubscriber();
        addTokenRequestedSubscriber();
        addUserFromTokenRequestedSubscriber();
    }

    private void addCleanAccountManagementRequestedSubscriber() {
        messageQueue.addHandler(QueueNames.CLEAN_TOKEN_MANAGEMENT_REQUESTED,
                (event) -> repository = new Repository());
    }

    public void addTokenRequestedSubscriber() {
        messageQueue.addHandler(QueueNames.TOKENS_REQUESTED,
                (event) -> {
                    TokenRequest tokenRequest = event.getArgument(0, TokenRequest.class);
                    logger.log(Level.INFO, "TOKENS_REQUESTED tokenRequest(" + tokenRequest + ")");
                    int numOfTokens = tokenRequest.getNumberOfTokens();
                    if (numOfTokens < MINIMUM_NUM_OF_TOKENS || numOfTokens > MAXIMUM_NUM_OF_TOKENS) {
                        logger.log(Level.INFO, "TOKENS_REQUESTED incorrect number(" + tokenRequest.getNumberOfTokens() + ")");
                        messageQueue.publish(QueueNames.TOKENS_RETURNED, new Event(new Object[]{null, "Incorrect number of tokens request. Allowed range [1,6]"}));
                    } else {
                        List<String> tokens = generateTokens(tokenRequest);
                        logger.log(Level.INFO, "TOKENS_REQUESTED generated token(" + tokens + ")");
                        messageQueue.publish(QueueNames.TOKENS_RETURNED, new Event(new Object[]{tokens, "Tokens successfully generated"}));
                    }
                });
    }

    /**
     * This method adds a subscriber to the message queue to handle TOKENS_REQUESTED events.
     * When a TOKENS_REQUESTED event is received, the method retrieves the number of tokens requested from the event's
     * TokenRequest object and checks if it is within the allowed range of [1,6]. If the number of tokens is not within this range,
     * a TOKENS_RETURNED event is published with a null token list and an error message indicating that the request was invalid.
     * If the number of tokens is valid, a list of tokens is generated and a TOKENS_RETURNED event is published with the list of tokens and a success message
     * @return
     */
    public void addUserFromTokenRequestedSubscriber() {
        messageQueue.addHandler(QueueNames.USER_FROM_TOKEN_REQUESTED,
                (event) -> {
                    String token = event.getArgument(0, String.class);
                    logger.log(Level.INFO, "USER_FROM_TOKEN_REQUESTED token(" + token + ")");
                    User user = repository.findUserAndRemoveToken(token);
                    if (user != null) {
                        logger.log(Level.INFO, "USER_FROM_TOKEN_REQUESTED user(" + user + ")");
                        messageQueue.publish(QueueNames.USER_FROM_TOKEN_RETURNED, new Event(new Object[]{user, "User successfully retrieved"}));
                    } else {
                        logger.log(Level.INFO, "USER_FROM_TOKEN_REQUESTED user null");
                        messageQueue.publish(QueueNames.USER_FROM_TOKEN_RETURNED, new Event(new Object[]{null, "Invalid token"}));
                    }
                });
    }

    private List<String> generateTokens(TokenRequest tokenRequest) {
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < tokenRequest.getNumberOfTokens(); i++) {
            tokens.add(UUID.randomUUID().toString());
        }
        repository.put(tokenRequest.getUser(), tokens);
        return tokens;
    }
}
