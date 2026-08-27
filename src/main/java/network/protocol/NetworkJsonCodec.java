package network.protocol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class NetworkJsonCodec {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String encode(NetworkMessage message) throws JsonProcessingException {
        return objectMapper.writeValueAsString(message);
    }

    public NetworkMessage decode(String json) throws JsonProcessingException {
        return objectMapper.readValue(json, NetworkMessage.class);
    }
}
