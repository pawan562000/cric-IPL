package io.javabrains.ipldashboard.data;

import java.io.File;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;

import io.javabrains.ipldashboard.model.Match;

@Configuration
@EnableBatchProcessing
public class BatchConfig {
	
//	@Value()
//	private String filePath;

//	File file = new File(filePath);
	private final String[] FIELD_NAMES = new String[] { "id", "city", "date", "player_of_match", "venue",
			"neutral_venue", "team1", "team2", "toss_winner", "toss_decision", "winner", "result", "result_margin",
			"eliminator", "method", "umpire1", "umpire2" };

	@Autowired
	public JobBuilderFactory jobBuilderFactory;

	@Autowired
	public StepBuilderFactory stepBuilderFactory;

	
	
//	@Bean
//	public FlatFileItemReader<MatchInput> reader() {
//		return new FlatFileItemReaderBuilder<MatchInput>().name("MatchItemReader")
//				.resource(new ClassPathResource("match-data.csv")).delimited().names(FIELD_NAMES)
//				.fieldSetMapper(new BeanWrapperFieldSetMapper<MatchInput>() {
//					{
//						setTargetType(MatchInput.class);
//					}
//				}).build();
//	}
	
    @Bean
    public FlatFileItemReader<MatchInput> reader() {
        FlatFileItemReader<MatchInput> itemReader = new FlatFileItemReader<>();
        itemReader.setResource(new FileSystemResource("src/main/resources/match-data.csv"));
        itemReader.setName("csvReader");
        itemReader.setLineMapper(lineMapper());
        return itemReader;
    }
    private LineMapper<MatchInput> lineMapper() {
        DefaultLineMapper<MatchInput> lineMapper = new DefaultLineMapper<>();

        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setDelimiter(",");
        lineTokenizer.setStrict(false);
        lineTokenizer.setNames("id", "city", "date", "player_of_match", "venue",
    			"neutral_venue", "team1", "team2", "toss_winner", "toss_decision", "winner", "result", "result_margin",
    			"eliminator", "method", "umpire1", "umpire2");

        BeanWrapperFieldSetMapper<MatchInput> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(MatchInput.class);

        lineMapper.setLineTokenizer(lineTokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);
        return lineMapper;

    }

	@Bean
	public MatchDataProcessor processor() {
		return new MatchDataProcessor();
	}

	@Bean
	public JdbcBatchItemWriter<Match> writer(DataSource dataSource) {
		return new JdbcBatchItemWriterBuilder<Match>()
				.itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
				.sql("INSERT INTO match (id, city, date, player_of_match, venue, team1, team2, toss_winner, toss_decision, match_winner, result, result_margin, umpire1, umpire2) "
						+ " VALUES (:id, :city, :date, :playerOfMatch, :venue, :team1, :team2, :tossWinner, :tossDecision, :matchWinner, :result, :resultMargin, :umpire1, :umpire2)")
				.dataSource(dataSource).build();
	}

	@Bean
	public Job importUserJob(JobCompletionNotificationListener listener, Step step1) {
		return jobBuilderFactory.get("importUserJob").incrementer(new RunIdIncrementer()).listener(listener).flow(step1)
				.end().build();
	}

	@Bean
	public Step step1(JdbcBatchItemWriter<Match> writer) {
		return stepBuilderFactory.get("step1").<MatchInput, Match>chunk(10).reader(reader()).processor(processor())
				.writer(writer).build();
	}
}