
package net._139130;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the net._139130 package.
 * &lt;p&gt;An ObjectFactory allows you to programatically
 * construct new instances of the Java representation
 * for XML content. The Java representation of XML
 * content can consist of schema derived interfaces
 * and classes representing the binding of schema
 * type definitions, element declarations and model
 * groups.  Factory methods for each of these are
 * provided in this class.
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _BindChannelChannelNum_QNAME = new QName("http://www.139130.net", "ChannelNum");
    private final static QName _BindChannelCarrier_QNAME = new QName("http://www.139130.net", "Carrier");
    private final static QName _BindChannelSendType_QNAME = new QName("http://www.139130.net", "SendType");
    private final static QName _MOMsgPhone_QNAME = new QName("http://www.139130.net", "Phone");
    private final static QName _MOMsgContent_QNAME = new QName("http://www.139130.net", "Content");
    private final static QName _MOMsgSpecNumber_QNAME = new QName("http://www.139130.net", "SpecNumber");
    private final static QName _MOMsgServiceType_QNAME = new QName("http://www.139130.net", "ServiceType");
    private final static QName _MOMsgReserve_QNAME = new QName("http://www.139130.net", "Reserve");
    private final static QName _AccountInfoAccount_QNAME = new QName("http://www.139130.net", "Account");
    private final static QName _AccountInfoName_QNAME = new QName("http://www.139130.net", "Name");
    private final static QName _AccountInfoIdentify_QNAME = new QName("http://www.139130.net", "Identify");
    private final static QName _AccountInfoBizNames_QNAME = new QName("http://www.139130.net", "BizNames");
    private final static QName _AccountInfoUserbrief_QNAME = new QName("http://www.139130.net", "Userbrief");
    private final static QName _BusinessTypeStartTime_QNAME = new QName("http://www.139130.net", "StartTime");
    private final static QName _BusinessTypeEndTime_QNAME = new QName("http://www.139130.net", "EndTime");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: net._139130
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link PostSingle }
     */
    public PostSingle createPostSingle() {
        return new PostSingle();
    }

    /**
     * Create an instance of {@link PostSingleResponse }
     */
    public PostSingleResponse createPostSingleResponse() {
        return new PostSingleResponse();
    }

    /**
     * Create an instance of {@link PostGroup }
     */
    public PostGroup createPostGroup() {
        return new PostGroup();
    }

    /**
     * Create an instance of {@link MessageData }
     */
    public MessageData createMessageData() {
        return new MessageData();
    }

    /**
     * Create an instance of {@link PostGroupResponse }
     */
    public PostGroupResponse createPostGroupResponse() {
        return new PostGroupResponse();
    }

    /**
     * Create an instance of {@link FindResponse }
     */
    public FindResponse createFindResponse() {
        return new FindResponse();
    }

    /**
     * Create an instance of {@link FindResponseResponse }
     */
    public FindResponseResponse createFindResponseResponse() {
        return new FindResponseResponse();
    }

    /**
     * Create an instance of {@link MTResponse }
     */
    public MTResponse createMTResponse() {
        return new MTResponse();
    }

    /**
     * Create an instance of {@link GetBusinessType }
     */
    public GetBusinessType createGetBusinessType() {
        return new GetBusinessType();
    }

    /**
     * Create an instance of {@link GetBusinessTypeResponse }
     */
    public GetBusinessTypeResponse createGetBusinessTypeResponse() {
        return new GetBusinessTypeResponse();
    }

    /**
     * Create an instance of {@link BusinessType }
     */
    public BusinessType createBusinessType() {
        return new BusinessType();
    }

    /**
     * Create an instance of {@link SetMedias }
     */
    public SetMedias createSetMedias() {
        return new SetMedias();
    }

    /**
     * Create an instance of {@link SetMediasResponse }
     */
    public SetMediasResponse createSetMediasResponse() {
        return new SetMediasResponse();
    }

    /**
     * Create an instance of {@link MediaItems }
     */
    public MediaItems createMediaItems() {
        return new MediaItems();
    }

    /**
     * Create an instance of {@link GetAccountInfo }
     */
    public GetAccountInfo createGetAccountInfo() {
        return new GetAccountInfo();
    }

    /**
     * Create an instance of {@link GetAccountInfoResponse }
     */
    public GetAccountInfoResponse createGetAccountInfoResponse() {
        return new GetAccountInfoResponse();
    }

    /**
     * Create an instance of {@link AccountInfo }
     */
    public AccountInfo createAccountInfo() {
        return new AccountInfo();
    }

    /**
     * Create an instance of {@link ModifyPassword }
     */
    public ModifyPassword createModifyPassword() {
        return new ModifyPassword();
    }

    /**
     * Create an instance of {@link ModifyPasswordResponse }
     */
    public ModifyPasswordResponse createModifyPasswordResponse() {
        return new ModifyPasswordResponse();
    }

    /**
     * Create an instance of {@link Post }
     */
    public Post createPost() {
        return new Post();
    }

    /**
     * Create an instance of {@link MTPacks }
     */
    public MTPacks createMTPacks() {
        return new MTPacks();
    }

    /**
     * Create an instance of {@link PostResponse }
     */
    public PostResponse createPostResponse() {
        return new PostResponse();
    }

    /**
     * Create an instance of {@link GsmsResponse }
     */
    public GsmsResponse createGsmsResponse() {
        return new GsmsResponse();
    }

    /**
     * Create an instance of {@link GetResponse }
     */
    public GetResponse createGetResponse() {
        return new GetResponse();
    }

    /**
     * Create an instance of {@link GetResponseResponse }
     */
    public GetResponseResponse createGetResponseResponse() {
        return new GetResponseResponse();
    }

    /**
     * Create an instance of {@link GetMOMessage }
     */
    public GetMOMessage createGetMOMessage() {
        return new GetMOMessage();
    }

    /**
     * Create an instance of {@link GetMOMessageResponse }
     */
    public GetMOMessageResponse createGetMOMessageResponse() {
        return new GetMOMessageResponse();
    }

    /**
     * Create an instance of {@link MOMsg }
     */
    public MOMsg createMOMsg() {
        return new MOMsg();
    }

    /**
     * Create an instance of {@link FindReport }
     */
    public FindReport createFindReport() {
        return new FindReport();
    }

    /**
     * Create an instance of {@link FindReportResponse }
     */
    public FindReportResponse createFindReportResponse() {
        return new FindReportResponse();
    }

    /**
     * Create an instance of {@link MTReport }
     */
    public MTReport createMTReport() {
        return new MTReport();
    }

    /**
     * Create an instance of {@link GetReport }
     */
    public GetReport createGetReport() {
        return new GetReport();
    }

    /**
     * Create an instance of {@link GetReportResponse }
     */
    public GetReportResponse createGetReportResponse() {
        return new GetReportResponse();
    }

    /**
     * Create an instance of {@link PostMass }
     */
    public PostMass createPostMass() {
        return new PostMass();
    }

    /**
     * Create an instance of {@link PostMassResponse }
     */
    public PostMassResponse createPostMassResponse() {
        return new PostMassResponse();
    }

    /**
     * Create an instance of {@link ArrayOfMediaItems }
     */
    public ArrayOfMediaItems createArrayOfMediaItems() {
        return new ArrayOfMediaItems();
    }

    /**
     * Create an instance of {@link BindChannel }
     */
    public BindChannel createBindChannel() {
        return new BindChannel();
    }

    /**
     * Create an instance of {@link ArrayOfBindChannel }
     */
    public ArrayOfBindChannel createArrayOfBindChannel() {
        return new ArrayOfBindChannel();
    }

    /**
     * Create an instance of {@link ArrayOfString }
     */
    public ArrayOfString createArrayOfString() {
        return new ArrayOfString();
    }

    /**
     * Create an instance of {@link ArrayOfMessageData }
     */
    public ArrayOfMessageData createArrayOfMessageData() {
        return new ArrayOfMessageData();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     *
     * @param value Java instance representing xml element's value.
     * @return the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://www.139130.net", name = "ChannelNum", scope = BindChannel.class)
    public JAXBElement<String> createBindChannelChannelNum(String value) {
        return new JAXBElement<String>(_BindChannelChannelNum_QNAME, String.class, BindChannel.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     *
     * @param value Java instance representing xml element's value.
     * @return the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://www.139130.net", name = "Carrier", scope = BindChannel.class)
    public JAXBElement<String> createBindChannelCarrier(String value) {
        return new JAXBElement<String>(_BindChannelCarrier_QNAME, String.class, BindChannel.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     *
     * @param value Java instance representing xml element's value.
     * @return the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://www.139130.net", name = "SendType", scope = BindChannel.class)
    public JAXBElement<String> createBindChannelSendType(String value) {
        return new JAXBElement<String>(_BindChannelSendType_QNAME, String.class, BindChannel.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     *
     * @param value Java instance representing xml element's value.
     * @return the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://www.139130.net", name = "Phone", scope = MOMsg.class)
    public JAXBElement<String> createMOMsgPhone(String value) {
        return new JAXBElement<String>(_MOMsgPhone_QNAME, String.class, MOMsg.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     *
     * @param value Java instance representing xml element's value.
     * @return the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://www.139130.net", name = "Content", scope = MOMsg.class)
    public JAXBElement<String> createMOMsgContent(String value) {
        return new JAXBElement<String>(_MOMsgContent_QNAME, String.class, MOMsg.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     *
     * @param value Java instance representing xml element's value.
     * @return the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://www.139130.net", name = "SpecNumber", scope = MOMsg.class)
    public JAXBElement<String> createMOMsgSpecNumber(String value) {
        return new JAXBElement<String>(_MOMsgSpecNumber_QNAME, String.class, MOMsg.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     *
     * @param value Java instance representing xml element's value.
     * @return the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://www.139130.net", name = "ServiceType", scope = MOMsg.class)
    public JAXBElement<String> createMOMsgServiceType(String value) {
        return new JAXBElement<String>(_MOMsgServiceType_QNAME, String.class, MOMsg.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     *
     * @param value Java instance representing xml element's value.
     * @return the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://www.139130.net", name = "Reserve", scope = MOMsg.class)
    public JAXBElement<String> createMOMsgReserve(String value) {
        return new JAXBElement<String>(_MOMsgReserve_QNAME, String.class, MOMsg.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     *
     * @param value Java instance representing xml element's value.
     * @return the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://www.139130.net", name = "Account", scope = AccountInfo.class)
    public JAXBElement<String> createAccountInfoAccount(String value) {
        return new JAXBElement<String>(_AccountInfoAccount_QNAME, String.class, AccountInfo.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     *
     * @param value Java instance representing xml element's value.
     * @return the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://www.139130.net", name = "Name", scope = AccountInfo.class)
    public JAXBElement<String> createAccountInfoName(String value) {
        return new JAXBElement<String>(_AccountInfoName_QNAME, String.class, AccountInfo.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     *
     * @param value Java instance representing xml element's value.
     * @return the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://www.139130.net", name = "Identify", scope = AccountInfo.class)
    public JAXBElement<String> createAccountInfoIdentify(String value) {
        return new JAXBElement<String>(_AccountInfoIdentify_QNAME, String.class, AccountInfo.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ArrayOfString }{@code >}
     *
     * @param value Java instance representing xml element's value.
     * @return the new instance of {@link JAXBElement }{@code <}{@link ArrayOfString }{@code >}
     */
    @XmlElementDecl(namespace = "http://www.139130.net", name = "BizNames", scope = AccountInfo.class)
    public JAXBElement<ArrayOfString> createAccountInfoBizNames(ArrayOfString value) {
        return new JAXBElement<ArrayOfString>(_AccountInfoBizNames_QNAME, ArrayOfString.class, AccountInfo.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     *
     * @param value Java instance representing xml element's value.
     * @return the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://www.139130.net", name = "Userbrief", scope = AccountInfo.class)
    public JAXBElement<String> createAccountInfoUserbrief(String value) {
        return new JAXBElement<String>(_AccountInfoUserbrief_QNAME, String.class, AccountInfo.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     *
     * @param value Java instance representing xml element's value.
     * @return the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://www.139130.net", name = "Reserve", scope = AccountInfo.class)
    public JAXBElement<String> createAccountInfoReserve(String value) {
        return new JAXBElement<String>(_MOMsgReserve_QNAME, String.class, AccountInfo.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     *
     * @param value Java instance representing xml element's value.
     * @return the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://www.139130.net", name = "Name", scope = BusinessType.class)
    public JAXBElement<String> createBusinessTypeName(String value) {
        return new JAXBElement<String>(_AccountInfoName_QNAME, String.class, BusinessType.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     *
     * @param value Java instance representing xml element's value.
     * @return the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://www.139130.net", name = "StartTime", scope = BusinessType.class)
    public JAXBElement<String> createBusinessTypeStartTime(String value) {
        return new JAXBElement<String>(_BusinessTypeStartTime_QNAME, String.class, BusinessType.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     *
     * @param value Java instance representing xml element's value.
     * @return the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://www.139130.net", name = "EndTime", scope = BusinessType.class)
    public JAXBElement<String> createBusinessTypeEndTime(String value) {
        return new JAXBElement<String>(_BusinessTypeEndTime_QNAME, String.class, BusinessType.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     *
     * @param value Java instance representing xml element's value.
     * @return the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://www.139130.net", name = "Phone", scope = MessageData.class)
    public JAXBElement<String> createMessageDataPhone(String value) {
        return new JAXBElement<String>(_MOMsgPhone_QNAME, String.class, MessageData.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     *
     * @param value Java instance representing xml element's value.
     * @return the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://www.139130.net", name = "Content", scope = MessageData.class)
    public JAXBElement<String> createMessageDataContent(String value) {
        return new JAXBElement<String>(_MOMsgContent_QNAME, String.class, MessageData.class, value);
    }

}
