package network.protocol.plants;

import java.util.ArrayList;
import java.util.List;

public class PlantOwnershipResponse {
    private boolean success;
    private String message;
    private List<Integer> unlockedPlantIds =
            new ArrayList<>();

    public PlantOwnershipResponse() {
    }

    public PlantOwnershipResponse(
            boolean success,
            String message,
            List<Integer> unlockedPlantIds
    ) {
        this.success = success;
        this.message = message;
        setUnlockedPlantIds(unlockedPlantIds);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<Integer> getUnlockedPlantIds() {
        return unlockedPlantIds;
    }

    public void setUnlockedPlantIds(
            List<Integer> unlockedPlantIds
    ) {
        this.unlockedPlantIds =
                unlockedPlantIds == null
                        ? new ArrayList<>()
                        : new ArrayList<>(unlockedPlantIds);
    }
}
