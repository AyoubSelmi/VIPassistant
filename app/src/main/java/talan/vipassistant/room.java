package talan.vipassistant;

public class room {
    private String roomName;
    private String Corner1_coordinates;
    private String Corner2_coordinates;
    private String Corner3_coordinates;
    private String Corner4_coordinates;

    public String getCorner1_coordinates() {
        return Corner1_coordinates;
    }

    public void setCorner1_coordinates(String corner1_coordinates) {
        Corner1_coordinates = corner1_coordinates;
    }

    public String getCorner2_coordinates() {
        return Corner2_coordinates;
    }

    public void setCorner2_coordinates(String corner2_coordinates) {
        Corner2_coordinates = corner2_coordinates;
    }

    public String getCorner3_coordinates() {
        return Corner3_coordinates;
    }

    public void setCorner3_coordinates(String corner3_coordinates) {
        Corner3_coordinates = corner3_coordinates;
    }

    public String getCorner4_coordinates() {
        return Corner4_coordinates;
    }

    public void setCorner4_coordinates(String corner4_coordinates) {
        Corner4_coordinates = corner4_coordinates;
    }

    public room() {
    }


    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }
}
