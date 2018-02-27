
# Telão interativo estilo SPTV a partir do Twitter


[![Build Status](https://travis-ci.org/willianantunes/telao-interativo-sptv.svg?branch=master)](https://travis-ci.org/willianantunes/telao-interativo-sptv)

---

Parte final do tutorial que foi iniciado a partir do repositório [twitter-sniffer](https://github.com/willianantunes/twitter-sniffer) e da postagem _[Apache Camel e Twitter: Orquestrando mensagens](http://willianantunes.blogspot.com.br/2017/05/apache-camel-e-twitter-orquestrando.html)_. O projeto foi totalmente atualizado tanto para casar com a última versão existente das libs quanto para ajeitá-lo de uma maneira geral (inclusão de testes, configs centralizadas, organização de rotas, etc.).

Acesse meu blog e conheça mais os pormenores desse projeto na postagem _[Telão interativo estilo SPTV com Apache Camel](http://willianantunes.blogspot.com.br/2018/02/telao-interativo-sptv-apache-camel.html)_.


![Executando projeto e vendo resultados](https://github.com/willianantunes/telao-interativo-sptv/blob/master/examples/how-to-run-2018-02-26%2005-57.gif?raw=true)

## Preparando ambiente de desenvolvimento

Para brincar com o projeto é necessário ter o ActiveMQ rodando. Na primeira postagem que fiz expliquei como configurá-lo para testar o projeto. Para deixar as coisas mais simples, sugiro usar um container via Docker:

    docker run --name='activemq' -itd --rm \
    -p 8161:8161 -p 61616:61616 -p 61613:61613 \
    -e 'ACTIVEMQ_STATIC_QUEUES=Tweets.Trends' webcenter/activemq:latest

A fila `Tweets.Trends` é utilizada no fluxo de controle dos tweets.

Para rodar o projeto, execute o comando:

    mvn spring-boot:run

E depois acesse o endereço `http://localhost:8095/telao-interativo-sptv.html` .

## Como realizar o build

Para construir o JAR rodando apenas os testes unitários execute o comando:

    maven clean package

Com teste de integração:

    maven clean verify

Para executar o JAR gerado:

    java -jar target/telao-interativo-sptv.jar

Se desejar alterar a palavra chave da busca no Twitter, você até pode alterar o arquivo *application.yml* e construir tudo de novo, mas como estamos com Spring Boot, podemos [passar como parâmetro](https://docs.spring.io/spring-boot/docs/1.5.9.RELEASE/reference/html/howto-properties-and-configuration.html#howto-set-active-spring-profiles) alterando o comportamento. Exemplo:

    java -jar -Dcustom.twitter.keywords="#TheWalkingDead" target/telao-interativo-sptv.jar

## Testes unitários e de integração

Além de testes com mocks, o projeto também contempla teste de integração considerando o ActiveMQ. Para testar a classe `PrepareTweetsAndEvaluateThemRoutesIT` por exemplo, execute o seguinte na linha de comando na raiz do projeto:

	mvn -Dit.test=PrepareTweetsAndEvaluateThemRoutesIT integration-test

É bom lembrar que por ser de integração terá que passar pelos unitários. Uma opção é interpretá-lo como unitário pelo comando abaixo, assim exclusivamente ele será executado:

    mvn -Dtest=PrepareTweetsAndEvaluateThemRoutesIT test

No teste é criado um container específico para avaliar a integração com o ActiveMQ e também são gerados os tweets do arquivo CSV que está em `src/test/resources` no banco de dados H2.

### Simulando WebSocket sem habilitar desenvolvimento em sua conta no Twitter

Expliquei um passo-a-passo no YouTube. Acesse no link abaixo:

[![Telão Interativo SPTV](http://img.youtube.com/vi/jgDDEfS8rZw/0.jpg)](http://www.youtube.com/watch?v=jgDDEfS8rZw "Telão Interativo estilo SPTV com Apache Camel")

### Erros conhecidos

Os testes funcionam OK no Linux, menos no Windows. Se você tentou construir pelo Windows talvez tenha pego um erro no teste unitário da classe ReadQueueAndSaveEachMessageRouteTest pois o payload processado não casa com o esperado. O porquê é simples. No Windows ele interpreta quebra de linha com _carriage return_ e _line feed_ enquanto no teste está apenas com _line feed_. Pra passar, substitua todos *\n* por *\r\n* que o teste passa. 

Só não faço a correção definitiva pois fica como lição honesta pra quem seguir adiante com esse projeto para algum fim. Nunca é tarde para lembrar que é sempre bom testar no sistema operacional alvo!

# Veja no GloboPlay como funciona

Procure por _SP1 estreia novo estúdio e telões interativos_ ou acesse [aqui](https://globoplay.globo.com/v/5853464/) enquanto ainda estiver disponível.