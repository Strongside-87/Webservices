package dk.dtu.pay.tokenmanagement;

import dk.dtu.pay.utils.messaging.RabbitMQQueue;
import lombok.SneakyThrows;

/**
 * @author Muhammad
 */
 
public class StartUp {

    @SneakyThrows
    public static void main(String[] args) {
        new Service(RabbitMQQueue.getInstance());
    }

}
