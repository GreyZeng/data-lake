package git.snippet.analyzer;

import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;

import java.util.Properties;

public class DataAnalyzer {
    public static void main(String[] args) {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "192.168.100.130:9092");
        properties.setProperty("group.id", "snippet");
        //构建FlinkKafkaConsumer
        FlinkKafkaConsumer<String> myConsumer = new FlinkKafkaConsumer<>("mytopic", new SimpleStringSchema(), properties);
        //指定偏移量
        myConsumer.setStartFromLatest();
        final DataStream<String> stream = env.addSource(myConsumer);
        env.enableCheckpointing(5000);
        stream.print();
        try {
            env.execute("DataAnalyzer");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
