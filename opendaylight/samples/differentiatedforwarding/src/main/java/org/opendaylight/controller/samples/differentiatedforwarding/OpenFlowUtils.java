package org.opendaylight.controller.samples.differentiatedforwarding;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Dscp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwTosActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.nw.tos.action._case.SetNwTosActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.TunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.UdpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg0;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg5;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.ExtensionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.GeneralAugMatchNodesNodeTableFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.GeneralAugMatchNodesNodeTableFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.grouping.ExtensionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.list.grouping.ExtensionList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.list.grouping.ExtensionListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.DstChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxRegCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegLoadNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.NxRegLoadBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.nx.reg.load.DstBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxAugMatchNodesNodeTableFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxAugMatchNodesNodeTableFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxReg0Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxReg1Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxReg2Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxReg3Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxReg4Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxReg5Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxReg6Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxReg7Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.reg.grouping.NxmNxRegBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionResubmitNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.nx.action.resubmit.grouping.NxResubmit;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.nx.action.resubmit.grouping.NxResubmitBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * These are mainly borrowed from OVSDB Plugin project.
 *
 */
public class OpenFlowUtils {
    private static final Logger logger = LoggerFactory.getLogger(OpenFlowUtils.class);

    public static BigInteger getDpId(String mac){
        BigInteger dpId = new BigInteger(mac.replace(":", ""), 16);
        return dpId;
    }
    /**
     * Create EtherType Match
     *
     * @param matchBuilder  Map matchBuilder MatchBuilder Object without a match
     * @param etherType     Long EtherType
     * @return matchBuilder Map MatchBuilder Object with a match
     */
    public static MatchBuilder createEtherTypeMatch(MatchBuilder matchBuilder, EtherType etherType) {

        EthernetMatchBuilder ethernetMatch = new EthernetMatchBuilder();
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(etherType));
        ethernetMatch.setEthernetType(ethTypeBuilder.build());
        matchBuilder.setEthernetMatch(ethernetMatch.build());

        return matchBuilder;
    }

    /**
     * Create Ingress Port Match dpidLong, inPort
     *
     * @param matchBuilder  Map matchBuilder MatchBuilder Object without a match
     * @param dpidLong      Long the datapath ID of a switch/node
     * @param inPort        Long ingress port on a switch
     * @return matchBuilder Map MatchBuilder Object with a match
     */
    public static MatchBuilder createInPortMatch(MatchBuilder matchBuilder,
            Node openFlowMDNode,
            NodeConnector inPortMDNodeConnector) {

        matchBuilder.setInPort(inPortMDNodeConnector.getId());
        return matchBuilder;
    }

    /**
     *
     * Credit to Mahdu OVSDB Plugin
     *
     */
    public static class RegMatch {
        final Class<? extends NxmNxReg> reg;
        final Long value;
        public RegMatch(Class<? extends NxmNxReg> reg, Long value) {
            super();
            this.reg = reg;
            this.value = value;
        }
        public static RegMatch of(Class<? extends NxmNxReg> reg, Long value) {
            return new RegMatch(reg, value);
        }
    }

    /**
     *
     * Credit to Mahdu OVSDB Plugin
     *
     */
    public static MatchBuilder addNxRegMatch(MatchBuilder match,
                                     RegMatch... regMatches) {
        ArrayList<ExtensionList> extensions = new ArrayList<>();
        for (RegMatch rm : regMatches) {
            Class<? extends ExtensionKey> key;
            if (NxmNxReg0.class.equals(rm.reg)) {
                key = NxmNxReg0Key.class;
            } else if (NxmNxReg1.class.equals(rm.reg)) {
                key = NxmNxReg1Key.class;
            } else if (NxmNxReg2.class.equals(rm.reg)) {
                key = NxmNxReg2Key.class;
            } else if (NxmNxReg3.class.equals(rm.reg)) {
                key = NxmNxReg3Key.class;
            } else if (NxmNxReg4.class.equals(rm.reg)) {
                key = NxmNxReg4Key.class;
            } else if (NxmNxReg5.class.equals(rm.reg)) {
                key = NxmNxReg5Key.class;
            } else if (NxmNxReg6.class.equals(rm.reg)) {
                key = NxmNxReg6Key.class;
            } else {
                key = NxmNxReg7Key.class;
            }
            NxAugMatchNodesNodeTableFlow am =
                    new NxAugMatchNodesNodeTableFlowBuilder()
                .setNxmNxReg(new NxmNxRegBuilder()
                    .setReg(rm.reg)
                    .setValue(rm.value)
                    .build())
                .build();
            extensions.add(new ExtensionListBuilder()
                .setExtensionKey(key)
                .setExtension(new ExtensionBuilder()
                     .addAugmentation(NxAugMatchNodesNodeTableFlow.class, am)
                     .build())
                .build());
        }
        GeneralAugMatchNodesNodeTableFlow m =
                new GeneralAugMatchNodesNodeTableFlowBuilder()
            .setExtensionList(extensions)
            .build();
        match.addAugmentation(GeneralAugMatchNodesNodeTableFlow.class, m);
        return match;
    }

    public static MatchBuilder createRegMatch(MatchBuilder matchBuilder, Class<? extends NxmNxReg> reg, long regValue){
        addNxRegMatch(matchBuilder, new RegMatch(reg, Long.valueOf(regValue)));
        return matchBuilder;
    }

    /**
     * Tunnel ID Match Builder
     *
     * @param matchBuilder  MatchBuilder Object without a match yet
     * @param tunnelId      BigInteger representing a tunnel ID
     * @return matchBuilder Map MatchBuilder Object with a match
     */
    public static MatchBuilder createTunnelIDMatch(MatchBuilder matchBuilder, BigInteger tunnelId) {

        TunnelBuilder tunnelBuilder = new TunnelBuilder();
        tunnelBuilder.setTunnelId(tunnelId);
        matchBuilder.setTunnel(tunnelBuilder.build());

        return matchBuilder;
    }

    /**
     * @param matchBuilder MatchBuilder Object without a match yet
     * @param dstip        String containing an IPv4 prefix
     * @return matchBuilder Map Object with a match
     */
    public static MatchBuilder createDstL3IPv4Match(MatchBuilder matchBuilder, InetAddress dstAddr) {

        EthernetMatchBuilder eth = new EthernetMatchBuilder();
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(0x0800L));
        eth.setEthernetType(ethTypeBuilder.build());
        matchBuilder.setEthernetMatch(eth.build());

        Ipv4Prefix dstIp = new Ipv4Prefix(dstAddr.getHostAddress());
        Ipv4MatchBuilder ipv4match = new Ipv4MatchBuilder();
        ipv4match.setIpv4Destination(dstIp);

        matchBuilder.setLayer3Match(ipv4match.build());

        return matchBuilder;

    }

    /**
     * @param matchBuilder MatchBuilder Object without a match yet
     * @param srcip        String containing an IPv4 prefix
     * @return             matchBuilder Map Object with a match
     */
    public static MatchBuilder createSrcL3IPv4Match(MatchBuilder matchBuilder, InetAddress srcAddr) {

        EthernetMatchBuilder eth = new EthernetMatchBuilder();
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(0x0800L));
        eth.setEthernetType(ethTypeBuilder.build());
        matchBuilder.setEthernetMatch(eth.build());

        Ipv4Prefix srcIp = new Ipv4Prefix(srcAddr.getHostAddress());
        Ipv4MatchBuilder ipv4match = new Ipv4MatchBuilder();
        ipv4match.setIpv4Source(srcIp);

        matchBuilder.setLayer3Match(ipv4match.build());

        return matchBuilder;

    }

    /**
     *
     * @param matchBuilder
     * @param srcAddr
     * @param dstAddr
     * @return
     */
    public static MatchBuilder createSrcDstL3IPv4Match(MatchBuilder matchBuilder, InetAddress srcAddr, InetAddress dstAddr) {

        EthernetMatchBuilder eth = new EthernetMatchBuilder();
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(0x0800L));
        eth.setEthernetType(ethTypeBuilder.build());
        matchBuilder.setEthernetMatch(eth.build());

        Ipv4Prefix srcIp = new Ipv4Prefix(srcAddr.getHostAddress());
        Ipv4Prefix dstIp = new Ipv4Prefix(dstAddr.getHostAddress());
        Ipv4MatchBuilder ipv4match = new Ipv4MatchBuilder();
        ipv4match.setIpv4Source(srcIp);
        ipv4match.setIpv4Destination(dstIp);

        matchBuilder.setLayer3Match(ipv4match.build());

        return matchBuilder;

    }

    /**
     * Create Destination UDP Port Match
     *
     * @param matchBuilder MatchBuilder Object without a match yet
     * @param udpport      Integer representing a destination UDP port
     * @return             matchBuilder Map MatchBuilder Object with a match
     */
    public static MatchBuilder createDstPortUdpMatch(MatchBuilder matchBuilder, PortNumber udpport) {

        EthernetMatchBuilder ethType = new EthernetMatchBuilder();
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(0x0800L));
        ethType.setEthernetType(ethTypeBuilder.build());
        matchBuilder.setEthernetMatch(ethType.build());

        IpMatchBuilder ipmatch = new IpMatchBuilder();
        ipmatch.setIpProtocol((short) 17);
        matchBuilder.setIpMatch(ipmatch.build());

        PortNumber dstport = new PortNumber(udpport);
        UdpMatchBuilder udpmatch = new UdpMatchBuilder();

        udpmatch.setUdpDestinationPort(dstport);
        matchBuilder.setLayer4Match(udpmatch.build());

        return matchBuilder;
    }

    public static MatchBuilder createNwDscpMatch(MatchBuilder matchBuilder, short dscp){
        IpMatchBuilder ipMatchBuilder = new IpMatchBuilder();
        Dscp value = new Dscp(dscp);
        ipMatchBuilder.setIpDscp(value);
        matchBuilder.setIpMatch(ipMatchBuilder.build());

        return matchBuilder;
    }

    /**
     * Create Output Port Instruction
     *
     * @param ib       Map InstructionBuilder without any instructions
     * @param dpidLong Long the datapath ID of a switch/node
     * @param port     Long representing a port on a switch/node
     * @return ib InstructionBuilder Map with instructions
     */
    public static InstructionBuilder createOutputPortInstructions(InstructionBuilder ib, Node openFlowNode, NodeConnector outputPort) {

        logger.debug("createOutputPortInstructions() Node Connector ID is - Type=openflow: Node {} outPort {} ", openFlowNode, outputPort);

        List<Action> actionList = new ArrayList<Action>();
        ActionBuilder ab = new ActionBuilder();
        OutputActionBuilder oab = new OutputActionBuilder();
        oab.setOutputNodeConnector(outputPort.getId());

        ab.setAction(new OutputActionCaseBuilder().setOutputAction(oab.build()).build());
        ab.setOrder(0);
        ab.setKey(new ActionKey(0));
        actionList.add(ab.build());

        // Create an Apply Action
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());

        return ib;
    }

    /**
     * Create Set DSCP Instruction. Note this won't set the two least significant bits of ToS for OVS (i.e. ECN)
     *
     * @param ib        Map InstructionBuilder without any instructions
     * @param dscp
     * @return ib Map InstructionBuilder with instructions
     */
    public static InstructionBuilder createNwDscpInstructions(InstructionBuilder ib, short dscp) {

        List<Action> actionList = new ArrayList<Action>();
        ActionBuilder ab = new ActionBuilder();

        SetNwTosActionBuilder setNwTosActionBuilder = new SetNwTosActionBuilder();
        // ODL OF use the nw_tos_shifted field to cover the ECN. However, the field is still called ToS here.
        setNwTosActionBuilder.setTos((int) dscp);
        ab.setAction(new SetNwTosActionCaseBuilder().setSetNwTosAction(setNwTosActionBuilder.build()).build());
        ab.setOrder(0);
        ab.setKey(new ActionKey(0));
        actionList.add(ab.build());

        // Create an Apply Action
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        // Wrap our Apply Action in an Instruction
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());

        return ib;
    }

    public static InstructionBuilder createMarkDscpAndOutputInstructions(InstructionBuilder ib, short dscp, NodeConnector outputPort){
        List<Action> actionList = new ArrayList<Action>();

        ActionBuilder ab = new ActionBuilder();
        SetNwTosActionBuilder setNwTosActionBuilder = new SetNwTosActionBuilder();
        setNwTosActionBuilder.setTos((int) dscp);
        ab.setAction(new SetNwTosActionCaseBuilder().setSetNwTosAction(setNwTosActionBuilder.build()).build());
        ab.setOrder(0);
        ab.setKey(new ActionKey(0));
        actionList.add(ab.build());

        ActionBuilder ab2 = new ActionBuilder();
        OutputActionBuilder oab = new OutputActionBuilder();
        oab.setOutputNodeConnector(outputPort.getId());
        ab2.setAction(new OutputActionCaseBuilder().setOutputAction(oab.build()).build());
        ab2.setOrder(1);
        ab2.setKey(new ActionKey(1));
        actionList.add(ab2.build());

        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());

        return ib;
    }
    /**
     * Create NORMAL Reserved Port Instruction (packet_in)
     *
     * @param ib Map InstructionBuilder without any instructions
     * @return ib Map InstructionBuilder with instructions
     */

    public static InstructionBuilder createNormalInstructions(InstructionBuilder ib) {

        List<Action> actionList = new ArrayList<Action>();
        ActionBuilder ab = new ActionBuilder();

        OutputActionBuilder output = new OutputActionBuilder();
        Uri value = new Uri("NORMAL");
        output.setOutputNodeConnector(value);
        ab.setAction(new OutputActionCaseBuilder().setOutputAction(output.build()).build());
        ab.setOrder(0);
        ab.setKey(new ActionKey(0));
        actionList.add(ab.build());

        // Create an Apply Action
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        // Wrap our Apply Action in an Instruction
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());

        return ib;
    }

    /**
     * Create Send to Controller Reserved Port Instruction (packet_in)
     * It's important to set the output length to maximum, so the packet will
     * be sent completely.
     * @param ib Map InstructionBuilder without any instructions
     * @return ib Map InstructionBuilder with instructions
     *
     */

    public static InstructionBuilder createSendToControllerInstructions(InstructionBuilder ib) {

        List<Action> actionList = new ArrayList<Action>();
        ActionBuilder ab = new ActionBuilder();

        OutputActionBuilder output = new OutputActionBuilder();
        output.setMaxLength(0xffff);
        Uri value = new Uri("CONTROLLER");
        output.setOutputNodeConnector(value);
        ab.setAction(new OutputActionCaseBuilder().setOutputAction(output.build()).build());
        ab.setOrder(0);
        ab.setKey(new ActionKey(0));
        actionList.add(ab.build());

        // Create an Apply Action
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        // Wrap our Apply Action in an Instruction
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());

        return ib;
    }

    public static InstructionBuilder createIngressDscpMarkResubmitInstructions(InstructionBuilder ib, Class<? extends NxmNxReg> reg, long regValue, int dscp, Integer inPort, Short table) {
        List<Action> actionList = new ArrayList<Action>();


        Action regLoadAction = createSetRegAction(reg, regValue, 0);
        actionList.add(regLoadAction);

        Action dscpAction = createMarkDscpAction(dscp, 1);
        actionList.add(dscpAction);

        Action resubmitAction = createNxResubmitAction(inPort, table, 2);
        actionList.add(resubmitAction);


        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());

        return ib;
    }

    private static Action createSetRegAction(Class<? extends NxmNxReg> reg, long regValue, int order){
        ActionBuilder regActionBuilder = new ActionBuilder();
        DstChoice dstReg = new DstNxRegCaseBuilder().setNxReg(reg).build();

        NxRegLoadBuilder regLoadBuilder = new NxRegLoadBuilder();
        regLoadBuilder.setDst(new DstBuilder().setDstChoice(dstReg).setStart(0).setEnd(31).build());
        regLoadBuilder.setValue(BigInteger.valueOf(regValue));

        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action fuckingLongName = new NxActionRegLoadNodesNodeTableFlowApplyActionsCaseBuilder()
        .setNxRegLoad(regLoadBuilder.build()).build();

        regActionBuilder.setAction(fuckingLongName);
        regActionBuilder.setOrder(order);
        regActionBuilder.setKey(new ActionKey(order));

        return regActionBuilder.build();

    }

    private static Action createMarkDscpAction(int dscp, int order){
        ActionBuilder dscpActionBuilder = new ActionBuilder();
        SetNwTosActionBuilder setNwTosActionBuilder = new SetNwTosActionBuilder();
        setNwTosActionBuilder.setTos(dscp);
        dscpActionBuilder.setAction(new SetNwTosActionCaseBuilder().setSetNwTosAction(setNwTosActionBuilder.build()).build());
        dscpActionBuilder.setOrder(order);
        dscpActionBuilder.setKey(new ActionKey(order));
        return dscpActionBuilder.build();
    }

    private static Action createNxResubmitAction(Integer inPort, Short table, int order){
        NxResubmitBuilder builder = new NxResubmitBuilder();
        if (inPort != null) builder.setInPort(inPort);
        if (table != null)  builder.setTable(table);
        NxResubmit r = builder.build();
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action resubmitAction = new NxActionResubmitNodesNodeTableFlowApplyActionsCaseBuilder().setNxResubmit(r).build();
        ActionBuilder resubmitActionBuilder = new ActionBuilder();
        resubmitActionBuilder.setAction(resubmitAction);
        resubmitActionBuilder.setOrder(order);
        resubmitActionBuilder.setKey(new ActionKey(order));
        return resubmitActionBuilder.build();
    }


}
