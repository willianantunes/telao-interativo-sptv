

# Telão interativo estilo SPTV a partir do Twitter

Parte final do tutorial que foi iniciado a partir do repositório [twitter-sniffer](https://github.com/willianantunes/twitter-sniffer) e da postagem _[Apache Camel e Twitter: Orquestrando mensagens](http://willianantunes.blogspot.com.br/2017/05/apache-camel-e-twitter-orquestrando.html)_. O projeto foi totalmente atualizado tanto para casar com a última versão existente das libs quanto para ajeitá-lo de uma maneira geral (inclusão de testes, configs centralizadas, organização de rotas, etc.).

Acesse meu blog e conheça mais os pormenores desse projeto na postagem _Telão interativo estilo SPTV com Apache Camel_ (não escrevi ainda, mas assim que terminar esse projeto atualizo o README com links e mais detalhes).

## Preparando ambiente de desenvolvimento

Para brincar com o projeto é necessário ter o ActiveMQ rodando. Na primeira postagem que fiz expliquei como configurá-lo para testar o projeto. Para deixar as coisas mais simples, sugiro usar um container via Docker:

    docker run --name='activemq' -itd --rm \
    -p 8161:8161 -p 61616:61616 -p 61613:61613 \
    -e 'ACTIVEMQ_STATIC_QUEUES=Tweets.Trends' webcenter/activemq:latest

A fila `Tweets.Trends` é utilizada no fluxo de controle dos tweets.

## Como testar

Além de testes com mocks, o projeto também contempla teste de integração considerando o ActiveMQ. Para testar a classe `PrepareTweetsAndEvaluateThemRoutesIT` por exemplo, execute o seguinte na linha de comando na raiz do projeto:

	mvn -Dit.test=PrepareTweetsAndEvaluateThemRoutesIT integration-test

No teste é criado um container específico para avaliar a integração com o ActiveMQ e também são gerados os tweets do arquivo CSV que está em `src/test/resources` no banco de dados H2.

# Veja no GloboPlay como funciona

Procure por _SP1 estreia novo estúdio e telões interativos_ ou acesse [aqui](https://globoplay.globo.com/v/5853464/) enquanto ainda estiver disponível.