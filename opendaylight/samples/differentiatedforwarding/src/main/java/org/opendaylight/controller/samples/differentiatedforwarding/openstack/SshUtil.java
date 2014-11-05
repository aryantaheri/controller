package org.opendaylight.controller.samples.differentiatedforwarding.openstack;

import java.io.IOException;
import java.security.Security;
import java.util.concurrent.TimeUnit;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.opendaylight.controller.samples.differentiatedforwarding.openstack.ssh.CommandOutPut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.schmizz.sshj.Config;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;

public class SshUtil {

    private static Logger log = LoggerFactory.getLogger(SshUtil.class);
    // Remote intermediate devstack testing:> ssh aryan@haisen10 'ssh fedora@controller "sudo ip netns exec $NS ssh -i $KEY $VM_IP $CMD"'
    // Remote controller openstack         :> ssh fedora@controller "sudo ip netns exec $NS ssh -i $KEY $VM_IP $CMD"
    private static String CONTROLLER_HOST = "192.168.10.250";
    private static String CONTROLLER_USER = "fedora";
    private static String CONTROLLER_KEY = "/home/aryan/.ssh/id_rsa";

    private static String INTERMEDIATE_HOST = "haisen10.ux.uis.no";
    private static String INTERMEDIATE_USER = "aryan";
    private static String INTERMEDIATE_KEY = "/home/aryan/.ssh/id_rsa_simple";

    private static String VM_SSH_OPTIONS = "-o \'StrictHostKeyChecking no\' -o \'UserKnownHostsFile=/dev/null\' -o \'LogLevel=error\'";

    private static String INTERMEDIATE_SSH_CMD = "ssh " + CONTROLLER_HOST;

    public static void main(String[] args) throws IOException {

        String vmNameSpace = "qdhcp-fa2af016-1e18-465f-ac17-e835cc950e46";
        String vmKey = "/home/fedora/devstack/cloud-keypair.pem";
        String vmUser = "cirros";
        String vmIp = "10.0.0.13";
        String vmCmd = "top";

        execVmCmd(vmNameSpace, vmKey, vmUser, vmIp, vmCmd, true);

    }

    public static CommandOutPut execVmCmd(String vmNameSpace, String vmKeyLocation, String vmUser, String vmIp, String vmCmd, boolean useIntermediate) throws IOException{
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

    public static void execVmCmdAsync(String vmNameSpace, String vmKey, String vmUser, String vmIp, String vmCmd, String controllerOutPutFile, boolean useIntermediate) throws IOException{

    }
}
