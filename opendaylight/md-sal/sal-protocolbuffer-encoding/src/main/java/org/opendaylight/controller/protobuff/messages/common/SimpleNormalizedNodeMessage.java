// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: SimpleNormalizedNode.proto

package org.opendaylight.controller.protobuff.messages.common;

public final class SimpleNormalizedNodeMessage {
  private SimpleNormalizedNodeMessage() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
  }
  public interface NormalizedNodeXmlOrBuilder
      extends com.google.protobuf.MessageOrBuilder {

    // required string nodeIdentifier = 1;
    /**
     * <code>required string nodeIdentifier = 1;</code>
     */
    boolean hasNodeIdentifier();
    /**
     * <code>required string nodeIdentifier = 1;</code>
     */
    java.lang.String getNodeIdentifier();
    /**
     * <code>required string nodeIdentifier = 1;</code>
     */
    com.google.protobuf.ByteString
        getNodeIdentifierBytes();

    // required string xmlString = 2;
    /**
     * <code>required string xmlString = 2;</code>
     */
    boolean hasXmlString();
    /**
     * <code>required string xmlString = 2;</code>
     */
    java.lang.String getXmlString();
    /**
     * <code>required string xmlString = 2;</code>
     */
    com.google.protobuf.ByteString
        getXmlStringBytes();
  }
  /**
   * Protobuf type {@code org.opendaylight.controller.mdsal.NormalizedNodeXml}
   */
  public static final class NormalizedNodeXml extends
      com.google.protobuf.GeneratedMessage
      implements NormalizedNodeXmlOrBuilder {
    // Use NormalizedNodeXml.newBuilder() to construct.
    private NormalizedNodeXml(com.google.protobuf.GeneratedMessage.Builder<?> builder) {
      super(builder);
      this.unknownFields = builder.getUnknownFields();
    }
    private NormalizedNodeXml(boolean noInit) { this.unknownFields = com.google.protobuf.UnknownFieldSet.getDefaultInstance(); }

    private static final NormalizedNodeXml defaultInstance;
    public static NormalizedNodeXml getDefaultInstance() {
      return defaultInstance;
    }

    public NormalizedNodeXml getDefaultInstanceForType() {
      return defaultInstance;
    }

    private final com.google.protobuf.UnknownFieldSet unknownFields;
    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet
        getUnknownFields() {
      return this.unknownFields;
    }
    private NormalizedNodeXml(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      initFields();
      int mutable_bitField0_ = 0;
      com.google.protobuf.UnknownFieldSet.Builder unknownFields =
          com.google.protobuf.UnknownFieldSet.newBuilder();
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            default: {
              if (!parseUnknownField(input, unknownFields,
                                     extensionRegistry, tag)) {
                done = true;
              }
              break;
            }
            case 10: {
              bitField0_ |= 0x00000001;
              nodeIdentifier_ = input.readBytes();
              break;
            }
            case 18: {
              bitField0_ |= 0x00000002;
              xmlString_ = input.readBytes();
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(
            e.getMessage()).setUnfinishedMessage(this);
      } finally {
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return org.opendaylight.controller.protobuff.messages.common.SimpleNormalizedNodeMessage.internal_static_org_opendaylight_controller_mdsal_NormalizedNodeXml_descriptor;
    }

    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return org.opendaylight.controller.protobuff.messages.common.SimpleNormalizedNodeMessage.internal_static_org_opendaylight_controller_mdsal_NormalizedNodeXml_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              org.opendaylight.controller.protobuff.messages.common.SimpleNormalizedNodeMessage.NormalizedNodeXml.class, org.opendaylight.controller.protobuff.messages.common.SimpleNormalizedNodeMessage.NormalizedNodeXml.Builder.class);
    }

    public static com.google.protobuf.Parser<NormalizedNodeXml> PARSER =
        new com.google.protobuf.AbstractParser<NormalizedNodeXml>() {
      public NormalizedNodeXml parsePartialFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws com.google.protobuf.InvalidProtocolBufferException {
        return new NormalizedNodeXml(input, extensionRegistry);
      }
    };

    @java.lang.Override
    public com.google.protobuf.Parser<NormalizedNodeXml> getParserForType() {
      return PARSER;
    }

    private int bitField0_;
    // required string nodeIdentifier = 1;
    public static final int NODEIDENTIFIER_FIELD_NUMBER = 1;
    private java.lang.Object nodeIdentifier_;
    /**
     * <code>required string nodeIdentifier = 1;</code>
     */
    public boolean hasNodeIdentifier() {
      return ((bitField0_ & 0x00000001) == 0x00000001);
    }
    /**
     * <code>required string nodeIdentifier = 1;</code>
     */
    public java.lang.String getNodeIdentifier() {
      java.lang.Object ref = nodeIdentifier_;
      if (ref instanceof java.lang.String) {
        return (java.lang.String) ref;
      } else {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        if (bs.isValidUtf8()) {
          nodeIdentifier_ = s;
        }
        return s;
      }
    }
    /**
     * <code>required string nodeIdentifier = 1;</code>
     */
    public com.google.protobuf.ByteString
        getNodeIdentifierBytes() {
      java.lang.Object ref = nodeIdentifier_;
      if (ref instanceof java.lang.String) {
        com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        nodeIdentifier_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    // required string xmlString = 2;
    public static final int XMLSTRING_FIELD_NUMBER = 2;
    private java.lang.Object xmlString_;
    /**
     * <code>required string xmlString = 2;</code>
     */
    public boolean hasXmlString() {
      return ((bitField0_ & 0x00000002) == 0x00000002);
    }
    /**
     * <code>required string xmlString = 2;</code>
     */
    public java.lang.String getXmlString() {
      java.lang.Object ref = xmlString_;
      if (ref instanceof java.lang.String) {
        return (java.lang.String) ref;
      } else {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        if (bs.isValidUtf8()) {
          xmlString_ = s;
        }
        return s;
      }
    }
    /**
     * <code>required string xmlString = 2;</code>
     */
    public com.google.protobuf.ByteString
        getXmlStringBytes() {
      java.lang.Object ref = xmlString_;
      if (ref instanceof java.lang.String) {
        com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        xmlString_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    private void initFields() {
      nodeIdentifier_ = "";
      xmlString_ = "";
    }
    private byte memoizedIsInitialized = -1;
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized != -1) return isInitialized == 1;

      if (!hasNodeIdentifier()) {
        memoizedIsInitialized = 0;
        return false;
      }
      if (!hasXmlString()) {
        memoizedIsInitialized = 0;
        return false;
      }
      memoizedIsInitialized = 1;
      return true;
    }

    public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      getSerializedSize();
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        output.writeBytes(1, getNodeIdentifierBytes());
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        output.writeBytes(2, getXmlStringBytes());
      }
      getUnknownFields().writeTo(output);
    }

    private int memoizedSerializedSize = -1;
    public int getSerializedSize() {
      int size = memoizedSerializedSize;
      if (size != -1) return size;

      size = 0;
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        size += com.google.protobuf.CodedOutputStream
          .computeBytesSize(1, getNodeIdentifierBytes());
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        size += com.google.protobuf.CodedOutputStream
          .computeBytesSize(2, getXmlStringBytes());
      }
      size += getUnknownFields().getSerializedSize();
      memoizedSerializedSize = size;
      return size;
    }

    private static final long serialVersionUID = 0L;
    @java.lang.Override
    protected java.lang.Object writeReplace()
        throws java.io.ObjectStreamException {
      return super.writeReplace();
    }

    public static org.opendaylight.controller.protobuff.messages.common.SimpleNormalizedNodeMessage.NormalizedNodeXml parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static org.opendaylight.controller.protobuff.messages.common.SimpleNormalizedNodeMessage.NormalizedNodeXml parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static org.opendaylight.controller.protobuff.messages.common.SimpleNormalizedNodeMessage.NormalizedNodeXml parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static org.opendaylight.controller.protobuff.messages.common.SimpleNormalizedNodeMessage.NormalizedNodeXml parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static org.opendaylight.controller.protobuff.messages.common.SimpleNormalizedNodeMessage.NormalizedNodeXml parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return PARSER.parseFrom(input);
    }
    public static org.opendaylight.controller.protobuff.messages.common.SimpleNormalizedNodeMessage.NormalizedNodeXml parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseFrom(input, extensionRegistry);
    }
    public static org.opendaylight.controller.protobuff.messages.common.SimpleNormalizedNodeMessage.NormalizedNodeXml parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return PARSER.parseDelimitedFrom(input);
    }
    public static org.opendaylight.controller.protobuff.messages.common.SimpleNormalizedNodeMessage.NormalizedNodeXml parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseDelimitedFrom(input, extensionRegistry);
    }
    public static org.opendaylight.controller.protobuff.messages.common.SimpleNormalizedNodeMessage.NormalizedNodeXml parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return PARSER.parseFrom(input);
    }
    public static org.opendaylight.controller.protobuff.messages.common.SimpleNormalizedNodeMessage.NormalizedNodeXml parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseFrom(input, extensionRegistry);
    }

    public static Builder newBuilder() { return Builder.create(); }
    public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder(org.opendaylight.controller.protobuff.messages.common.SimpleNormalizedNodeMessage.NormalizedNodeXml prototype) {
      return newBuilder().mergeFrom(prototype);
    }
    public Builder toBuilder() { return newBuilder(this); }

    @java.lang.Override
    protected Builder newBuilderForType(
        com.google.protobuf.GeneratedMessage.BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }
    /**
     * Protobuf type {@code org.opendaylight.controller.mdsal.NormalizedNodeXml}
     */
    public static final class Builder extends
        com.google.protobuf.GeneratedMessage.Builder<Builder>
       implements org.opendaylight.controller.protobuff.messages.common.SimpleNormalizedNodeMessage.NormalizedNodeXmlOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor
          getDescriptor() {
        return org.opendaylight.controller.protobuff.messages.common.SimpleNormalizedNodeMessage.internal_static_org_opendaylight_controller_mdsal_NormalizedNodeXml_descriptor;
      }

      protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
          internalGetFieldAccessorTable() {
        return org.opendaylight.controller.protobuff.messages.common.SimpleNormalizedNodeMessage.internal_static_org_opendaylight_controller_mdsal_NormalizedNodeXml_fieldAccessorTable
            .ensureFieldAccessorsInitialized(
                org.opendaylight.controller.protobuff.messages.common.SimpleNormalizedNodeMessage.NormalizedNodeXml.class, org.opendaylight.controller.protobuff.messages.common.SimpleNormalizedNodeMessage.NormalizedNodeXml.Builder.class);
      }

      // Construct using org.opendaylight.controller.protobuff.messages.common.SimpleNormalizedNodeMessage.NormalizedNodeXml.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(
          com.google.protobuf.GeneratedMessage.BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }
      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders) {
        }
      }
      private static Builder create() {
        return new Builder();
      }

      public Builder clear() {
        super.clear();
        nodeIdentifier_ = "";
        bitField0_ = (bitField0_ & ~0x00000001);
        xmlString_ = "";
        bitField0_ = (bitField0_ & ~0x00000002);
        return this;
      }

      public Builder clone() {
        return create().mergeFrom(buildPartial());
      }

      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return org.opendaylight.controller.protobuff.messages.common.SimpleNormalizedNodeMessage.internal_static_org_opendaylight_controller_mdsal_NormalizedNodeXml_descriptor;
      }

      public org.opendaylight.controller.protobuff.messages.common.SimpleNormalizedNodeMessage.NormalizedNodeXml getDefaultInstanceForType() {
        return org.opendaylight.controller.protobuff.messages.common.SimpleNormalizedNodeMessage.NormalizedNodeXml.getDefaultInstance();
      }

      public org.opendaylight.controller.protobuff.messages.common.SimpleNormalizedNodeMessage.NormalizedNodeXml build() {
        org.opendaylight.controller.protobuff.messages.common.SimpleNormalizedNodeMessage.NormalizedNodeXml result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      public org.opendaylight.controller.protobuff.messages.common.SimpleNormalizedNodeMessage.NormalizedNodeXml buildPartial() {
        org.opendaylight.controller.protobuff.messages.common.SimpleNormalizedNodeMessage.NormalizedNodeXml result = new org.opendaylight.controller.protobuff.messages.common.SimpleNormalizedNodeMessage.NormalizedNodeXml(this);
        int from_bitField0_ = bitField0_;
        int to_bitField0_ = 0;
        if (((from_bitField0_ & 0x00000001) == 0x00000001)) {
          to_bitField0_ |= 0x00000001;
        }
        result.nodeIdentifier_ = nodeIdentifier_;
        if (((from_bitField0_ & 0x00000002) == 0x00000002)) {
          to_bitField0_ |= 0x00000002;
        }
        result.xmlString_ = xmlString_;
        result.bitField0_ = to_bitField0_;
        onBuilt();
        return result;
      }

      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof org.opendaylight.controller.protobuff.messages.common.SimpleNormalizedNodeMessage.NormalizedNodeXml) {
          return mergeFrom((org.opendaylight.controller.protobuff.messages.common.SimpleNormalizedNodeMessage.NormalizedNodeXml)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(org.opendaylight.controller.protobuff.messages.common.SimpleNormalizedNodeMessage.NormalizedNodeXml other) {
        if (other == org.opendaylight.controller.protobuff.messages.common.SimpleNormalizedNodeMessage.NormalizedNodeXml.getDefaultInstance()) return this;
        if (other.hasNodeIdentifier()) {
          bitField0_ |= 0x00000001;
          nodeIdentifier_ = other.nodeIdentifier_;
          onChanged();
        }
        if (other.hasXmlString()) {
          bitField0_ |= 0x00000002;
          xmlString_ = other.xmlString_;
          onChanged();
        }
        this.mergeUnknownFields(other.getUnknownFields());
        return this;
      }

      public final boolean isInitialized() {
        if (!hasNodeIdentifier()) {

          return false;
        }
        if (!hasXmlString()) {

          return false;
        }
        return true;
      }

      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        org.opendaylight.controller.protobuff.messages.common.SimpleNormalizedNodeMessage.NormalizedNodeXml parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (org.opendaylight.controller.protobuff.messages.common.SimpleNormalizedNodeMessage.NormalizedNodeXml) e.getUnfinishedMessage();
          throw e;
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }
      private int bitField0_;

      // required string nodeIdentifier = 1;
      private java.lang.Object nodeIdentifier_ = "";
      /**
       * <code>required string nodeIdentifier = 1;</code>
       */
      public boolean hasNodeIdentifier() {
        return ((bitField0_ & 0x00000001) == 0x00000001);
      }
      /**
       * <code>required string nodeIdentifier = 1;</code>
       */
      public java.lang.String getNodeIdentifier() {
        java.lang.Object ref = nodeIdentifier_;
        if (!(ref instanceof java.lang.String)) {
          java.lang.String s = ((com.google.protobuf.ByteString) ref)
              .toStringUtf8();
          nodeIdentifier_ = s;
          return s;
        } else {
          return (java.lang.String) ref;
        }
      }
      /**
       * <code>required string nodeIdentifier = 1;</code>
       */
      public com.google.protobuf.ByteString
          getNodeIdentifierBytes() {
        java.lang.Object ref = nodeIdentifier_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b =
              com.google.protobuf.ByteString.copyFromUtf8(
                  (java.lang.String) ref);
          nodeIdentifier_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }
      /**
       * <code>required string nodeIdentifier = 1;</code>
       */
      public Builder setNodeIdentifier(
          java.lang.String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  bitField0_ |= 0x00000001;
        nodeIdentifier_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>required string nodeIdentifier = 1;</code>
       */
      public Builder clearNodeIdentifier() {
        bitField0_ = (bitField0_ & ~0x00000001);
        nodeIdentifier_ = getDefaultInstance().getNodeIdentifier();
        onChanged();
        return this;
      }
      /**
       * <code>required string nodeIdentifier = 1;</code>
       */
      public Builder setNodeIdentifierBytes(
          com.google.protobuf.ByteString value) {
        if (value == null) {
    throw new NullPointerException();
  }
  bitField0_ |= 0x00000001;
        nodeIdentifier_ = value;
        onChanged();
        return this;
      }

      // required string xmlString = 2;
      private java.lang.Object xmlString_ = "";
      /**
       * <code>required string xmlString = 2;</code>
       */
      public boolean hasXmlString() {
        return ((bitField0_ & 0x00000002) == 0x00000002);
      }
      /**
       * <code>required string xmlString = 2;</code>
       */
      public java.lang.String getXmlString() {
        java.lang.Object ref = xmlString_;
        if (!(ref instanceof java.lang.String)) {
          java.lang.String s = ((com.google.protobuf.ByteString) ref)
              .toStringUtf8();
          xmlString_ = s;
          return s;
        } else {
          return (java.lang.String) ref;
        }
      }
      /**
       * <code>required string xmlString = 2;</code>
       */
      public com.google.protobuf.ByteString
          getXmlStringBytes() {
        java.lang.Object ref = xmlString_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b =
              com.google.protobuf.ByteString.copyFromUtf8(
                  (java.lang.String) ref);
          xmlString_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }
      /**
       * <code>required string xmlString = 2;</code>
       */
      public Builder setXmlString(
          java.lang.String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  bitField0_ |= 0x00000002;
        xmlString_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>required string xmlString = 2;</code>
       */
      public Builder clearXmlString() {
        bitField0_ = (bitField0_ & ~0x00000002);
        xmlString_ = getDefaultInstance().getXmlString();
        onChanged();
        return this;
      }
      /**
       * <code>required string xmlString = 2;</code>
       */
      public Builder setXmlStringBytes(
          com.google.protobuf.ByteString value) {
        if (value == null) {
    throw new NullPointerException();
  }
  bitField0_ |= 0x00000002;
        xmlString_ = value;
        onChanged();
        return this;
      }

      // @@protoc_insertion_point(builder_scope:org.opendaylight.controller.mdsal.NormalizedNodeXml)
    }

    static {
      defaultInstance = new NormalizedNodeXml(true);
      defaultInstance.initFields();
    }

    // @@protoc_insertion_point(class_scope:org.opendaylight.controller.mdsal.NormalizedNodeXml)
  }

  private static com.google.protobuf.Descriptors.Descriptor
    internal_static_org_opendaylight_controller_mdsal_NormalizedNodeXml_descriptor;
  private static
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internal_static_org_opendaylight_controller_mdsal_NormalizedNodeXml_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\032SimpleNormalizedNode.proto\022!org.openda" +
      "ylight.controller.mdsal\">\n\021NormalizedNod" +
      "eXml\022\026\n\016nodeIdentifier\030\001 \002(\t\022\021\n\txmlStrin" +
      "g\030\002 \002(\tBT\n5org.opendaylight.controller.p" +
      "rotobuff.messages.commonB\033SimpleNormaliz" +
      "edNodeMessage"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
      new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {
        public com.google.protobuf.ExtensionRegistry assignDescriptors(
            com.google.protobuf.Descriptors.FileDescriptor root) {
          descriptor = root;
          internal_static_org_opendaylight_controller_mdsal_NormalizedNodeXml_descriptor =
            getDescriptor().getMessageTypes().get(0);
          internal_static_org_opendaylight_controller_mdsal_NormalizedNodeXml_fieldAccessorTable = new
            com.google.protobuf.GeneratedMessage.FieldAccessorTable(
              internal_static_org_opendaylight_controller_mdsal_NormalizedNodeXml_descriptor,
              new java.lang.String[] { "NodeIdentifier", "XmlString", });
          return null;
        }
      };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        }, assigner);
  }

  // @@protoc_insertion_point(outer_class_scope)
}
