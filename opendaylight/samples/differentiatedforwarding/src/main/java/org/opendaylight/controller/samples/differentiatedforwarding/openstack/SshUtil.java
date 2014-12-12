package org.opendaylight.controller.samples.differentiatedforwarding.openstack;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;

import org.opendaylight.controller.samples.differentiatedforwarding.openstack.ssh.CommandOutPut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SshUtil {

    private static Logger log = LoggerFactory.getLogger(SshUtil.class);
    // Remote intermediate devstack testing:> ssh aryan@haisen10 'ssh fedora@controller "sudo ip netns exec $NS ssh -i $KEY $VM_IP $CMD"'
    // Remote controller openstack         :> ssh fedora@controller "sudo ip netns exec $NS ssh -i $KEY $VM_IP $CMD"
    private static String CONTROLLER_HOST = "nuc2"; // "192.168.10.250";
    private static String CONTROLLER_USER = "root"; // "fedora";
    private static String CONTROLLER_KEY = "/home/aryan/.ssh/id_rsa_simple";

    private static boolean useIntermediate = false;
    private static String INTERMEDIATE_HOST = "haisen10.ux.uis.no";
    private static String INTERMEDIATE_USER = "aryan";
    private static String INTERMEDIATE_KEY = "/home/aryan/.ssh/id_rsa_simple";

    public static String VM_SSH_OPTIONS = "-o \'StrictHostKeyChecking no\' -o \'UserKnownHostsFile=/dev/null\' -o \'LogLevel=error\'";

    private static String INTERMEDIATE_SSH_CMD = "ssh " + CONTROLLER_HOST;

    public static void main(String[] args) throws IOException {

        String vmNameSpace = "qdhcp-fa2af016-1e18-465f-ac17-e835cc950e46";
        String vmKey = "/home/fedora/devstack/cloud-keypair.pem";
        String vmUser = "cirros";
        String vmIp = "10.0.0.13";
        String vmCmd = "top";

        execVmCmd(vmNameSpace, vmKey, vmUser, vmIp, vmCmd);

    }

    public static CommandOutPut execVmCmd(String vmNameSpace, String vmKeyLocation, String vmUser, String vmIp, String vmCmd) throws IOException{
        String host = null;
        String user = null;
        String key = null;
        String cmdString = null;
        // Command to be executed in the controller node
        String controllerCmd = "sudo /sbin/ip netns exec " + vmNameSpace + " ssh " + VM_SSH_OPTIONS + " " + "-i " + vmKeyLocation + " " + vmUser + "@" + vmIp + " " + "\'" + vmCmd + "\'";

        if (useIntermediate) {
            host = INTERMEDIATE_HOST;
            user = INTERMEDIATE_USER;
            key = INTERMEDIATE_KEY;

            cmdString = "ssh " + " -i " + CONTROLLER_KEY + " " + CONTROLLER_USER + "@" + CONTROLLER_HOST + " " + "\"" + controllerCmd + "\"";
        } else {
            host = CONTROLLER_HOST;
            user = CONTROLLER_USER;
            key = CONTROLLER_KEY;

            cmdString = controllerCmd;
        }

        log.debug("cmdString: " + cmdString);
        final SSHClient ssh = new SSHClient();
        ssh.loadKnownHosts();
        ssh.connect(host);
        try {
            ssh.authPublickey(user, key);
            final Session session = ssh.startSession();
            try {
                final Command cmd = session.exec(cmdString);
                String output = IOUtils.readFully(cmd.getInputStream()).toString();
                String error = IOUtils.readFully(cmd.getErrorStream()).toString();
                log.info("execVmCmd vmIP {}, vmNS {}, vmCmd {}, vmCmdOutput {}, vmCmdError {}", vmIp, vmNameSpace, vmCmd, output, error);

                cmd.join(5, TimeUnit.SECONDS);
                log.info("execVmCmd Exit Status: {}", cmd.getExitStatus());

                if (cmd.getExitStatus() != 0)
                    log.error("execVmCmd cmd: {} Exit Status: {} Error msg: {}", cmdString, cmd.getExitStatus(), cmd.getExitErrorMessage());

                CommandOutPut cmdOutPut = new CommandOutPut(cmdString, output, error, cmd.getExitStatus(), cmd.getExitErrorMessage());
                return cmdOutPut;
            } finally {
                session.close();
            }
        } finally {
            ssh.disconnect();
            ssh.close();
        }
    }

    public static CommandOutPut execControllerCmd(String controllerCmd) throws IOException{
        String host = null;
        String user = null;
        String key = null;
        String cmdString = null;

        if (useIntermediate) {
            host = INTERMEDIATE_HOST;
            user = INTERMEDIATE_USER;
            key = INTERMEDIATE_KEY;

            cmdString = "ssh " + " -i " + CONTROLLER_KEY + " " + CONTROLLER_USER + "@" + CONTROLLER_HOST + " " + "\"" + controllerCmd + "\"";
        } else {
            host = CONTROLLER_HOST;
            user = CONTROLLER_USER;
            key = CONTROLLER_KEY;

            cmdString = controllerCmd;
        }

        log.debug("cmdString: " + cmdString);
        final SSHClient ssh = new SSHClient();
        ssh.loadKnownHosts();
        ssh.connect(host);
        try {
            ssh.authPublickey(user, key);
            final Session session = ssh.startSession();
            try {
                final Command cmd = session.exec(cmdString);
                String output = IOUtils.readFully(cmd.getInputStream()).toString();
                String error = IOUtils.readFully(cmd.getErrorStream()).toString();
                log.info("execControllerCmd controllerCmd {} vmCmdOutput \n {}, vmCmdError \n {}", controllerCmd, output, error);

                cmd.join(5, TimeUnit.SECONDS);
                log.info("execControllerCmd Exit Status: {}", cmd.getExitStatus());

                if (cmd.getExitStatus() != 0)
                    log.error("execControllerCmd cmd: {} Exit Status: {} Error msg: {}", cmdString, cmd.getExitStatus(), cmd.getExitErrorMessage());

                CommandOutPut cmdOutPut = new CommandOutPut(cmdString, output, error, cmd.getExitStatus(), cmd.getExitErrorMessage());
                return cmdOutPut;
            } finally {
                session.close();
            }
        } finally {
            ssh.disconnect();
            ssh.close();
        }
    }

    public static CommandOutPut execCmd(String hostName, String hostCmd) throws Exception {
        String host = hostName;
        String user = null;
        String key = null;
        String cmdString = null;

        if (useIntermediate) {
            host = INTERMEDIATE_HOST;
            user = INTERMEDIATE_USER;
            key = INTERMEDIATE_KEY;

            cmdString = "ssh " + " -i " + CONTROLLER_KEY + " " + CONTROLLER_USER + "@" + host + " " + "\"" + hostCmd + "\"";
        } else {
            user = CONTROLLER_USER;
            key = CONTROLLER_KEY;

            cmdString = hostCmd;
        }

        log.debug("cmdString: " + cmdString);
        final SSHClient ssh = new SSHClient();
        ssh.loadKnownHosts();
        ssh.connect(host);
        try {
            ssh.authPublickey(user, key);
            final Session session = ssh.startSession();
            try {
                final Command cmd = session.exec(cmdString);
                String output = IOUtils.readFully(cmd.getInputStream()).toString();
                String error = IOUtils.readFully(cmd.getErrorStream()).toString();
                log.info("execCmd hostName {} hostCmd {} cmdOutput {}, cmdError {}", host, hostCmd, output, error);

                cmd.join(5, TimeUnit.SECONDS);
                log.info("execCmd Exit Status: {}", cmd.getExitStatus());

                if (cmd.getExitStatus() != 0)
                    log.error("execCmd cmd: {} Exit Status: {} Error msg: {}", cmdString, cmd.getExitStatus(), cmd.getExitErrorMessage());

                CommandOutPut cmdOutPut = new CommandOutPut(cmdString, output, error, cmd.getExitStatus(), cmd.getExitErrorMessage());
                return cmdOutPut;
            } finally {
                session.close();
            }
        } finally {
            ssh.disconnect();
            ssh.close();
        }

    }

    public static void execVmCmdAsync(String vmNameSpace, String vmKey, String vmUser, String vmIp, String vmCmd, String controllerOutPutFile) throws IOException{

    }
}
