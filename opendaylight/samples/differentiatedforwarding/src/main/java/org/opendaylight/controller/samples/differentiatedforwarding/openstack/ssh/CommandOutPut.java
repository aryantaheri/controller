package org.opendaylight.controller.samples.differentiatedforwarding.openstack.ssh;

public class CommandOutPut {

    private String cmd;
    private String output;
    private String error;
    private int exitStatus = -10;
    private String exitErrorMessage;


//    name = ((city == null) || (city.getName() == null) ? "N/A" : city.getName());

    public CommandOutPut(String cmd, String output, String error, int exitStatus, String exitErrorMessage) {
        this.cmd = cmd;
        this.output = ((output == null) ? output : output.trim());
        this.error = ((error == null) ? error : error.trim());
        this.exitStatus = exitStatus;
        this.exitErrorMessage = ((exitErrorMessage == null) ? exitErrorMessage : exitErrorMessage.trim());
    }

    public String getOutput() {
        return output;
    }
    public void setOutput(String output) {
        this.output = output;
    }
    public String getError() {
        return error;
    }
    public void setError(String error) {
        this.error = error;
    }
    public int getExitStatus() {
        return exitStatus;
    }
    public void setExitStatus(int exitStatus) {
        this.exitStatus = exitStatus;
    }
    public String getExitErrorMessage() {
        return exitErrorMessage;
    }
    public void setExitErrorMessage(String exitErrorMessage) {
        this.exitErrorMessage = exitErrorMessage;
    }

    @Override
    public String toString() {
        return "\n Command: " + cmd +
                "\n ,Output: " + output +
                "\n ,Error: " + error +
                "\n ,ExitStatus: " + exitStatus +
                "\n ,ExitError:" + exitErrorMessage;
    }

}
