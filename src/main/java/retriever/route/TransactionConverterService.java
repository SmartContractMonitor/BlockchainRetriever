package retriever.route;

import decoder.Decoder;
import org.apache.camel.Body;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import retriever.apicaller.EtherScanCaller;
import retriever.db.DBHelper;


@Component
public class TransactionConverterService {

    @Autowired
    private DBHelper dbHelper;

    @Autowired
    private EtherScanCaller etherScanCaller;

    public String addMethodName(@Body String transaction) {
        JSONObject object = new JSONObject(transaction);

        if (object.isNull("to"))
            return transaction;

        String address = object.getString("to");
        String inputMethodData = Decoder.getInputMethodData(transaction);

        if (inputMethodData == null)
            return transaction;

        String methodName = dbHelper.findMethodInContract(address, inputMethodData);

        if (methodName == null) {
            String abi = etherScanCaller.getABI(address);
            if (abi == null || abi.equals("Contract source code not verified")) {
                return transaction;
            }
            dbHelper.addContractToDB(address, abi);
            methodName = Decoder.getFunctionName(abi, transaction);
        }

        if (methodName == null)
            return transaction;

        object.put("methodName", methodName);
        object.put("method", methodName + "@" + address);
        return object.toString();
    }
}

