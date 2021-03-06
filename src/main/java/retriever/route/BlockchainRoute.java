package retriever.route;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BlockchainRoute extends RouteBuilder {
    @Value("${node.address}")
    private String nodeAddress;

    @Value("${node.port}")
    private int nodePort;

    @Override
    public void configure() {
        from("web3j://http://"
                + nodeAddress + ":"
                + Integer.toString(nodePort)
                + "?operation=BLOCK_OBSERVABLE&fullTransactionObjects=true")
                .marshal().json(JsonLibrary.Gson)
                .convertBodyTo(String.class)
                .to("direct:dbInsertBlock");


        from("direct:dbInsertBlock")
                .bean(BlockConverterService.class, "convertBlockNumberToDec")
                .bean(BlockConverterService.class, "convertTimestampToDec")
                .to("direct:parseBlock")
                .to("mongodb:mongo?database=blockchain&collection=blocks&operation=insert");

        from("direct:parseBlock")
                .process(new TransactionParser());  //to "direct:dbInsertTransaction"

        from("direct:dbInsertTransaction")
                .bean(TransactionConverterService.class, "addMethodName")
                .to("mongodb:mongo?database=blockchain&collection=transactions&operation=insert");

        from("direct:dbInsertContract")
                .to("mongodb:mongo?database=blockchain&collection=contracts&operation=insert");


        from("direct:dbFindOneByQuery")
                .to("mongodb:mongo?database=blockchain&collection=blocks&operation=findOneByQuery");

        from("direct:dbContractFindOneByQuery")
                .to("mongodb:mongo?database=blockchain&collection=contracts&operation=findOneByQuery");

        from("direct:query")
                .to("mongodb:mongo?database=blockchain&collection=blocks&operation=aggregate");
    }
}
