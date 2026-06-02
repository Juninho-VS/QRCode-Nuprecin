# NFC-e QRCode Parser

Aplicacao Java 17 em Maven que acessa uma URL de QR Code de NFC-e, baixa a pagina HTML da nota e transforma os dados extraidos em JSON.

O projeto foi pensado para facilitar integracoes, automacoes e analise de notas fiscais eletronicas de consumidor, sem depender de processamento manual do HTML.

## Visao geral

O fluxo da aplicacao e simples:

1. Recebe uma URL de QR Code de NFC-e.
2. Faz a requisicao HTTP para a pagina da nota.
3. Analisa o HTML com `jsoup`.
4. Extrai dados da chave de acesso, emitente, consumidor, itens, totais e informacoes complementares.
5. Serializa o resultado em JSON com `Jackson`.

## Funcionalidades

- Leitura de URL de QR Code via argumento de linha de comando ou variavel de ambiente.
- Suporte a URLs com `|` nao escapado na query string.
- Extracao da chave de acesso, dados do emitente e do consumidor.
- Parse de itens, totais e informacoes complementares.
- Saida em JSON formatado e com nomes em `snake_case`.
- Testes automatizados para os pontos principais de parse.

## Tecnologias

- Java 17
- Maven
- `jsoup`
- `jackson-databind`
- JUnit 5

## Requisitos

- Java 17 ou superior
- Maven 3.8+ recomendado
- Acesso a internet para consultar a URL do QR Code

## Como executar

### 1. Clonar o projeto

```bash
git clone <URL_DO_REPOSITORIO>
cd nfce-qrcode-parser
```

### 2. Executar diretamente com Maven

Passe a URL do QR Code como argumento:

```bash
mvn -q exec:java -Dexec.args="<URL_QRCODE_NFCE>"
```

Ou defina a variavel de ambiente `NFC_E_QRCODE_URL`:

```bash
set NFC_E_QRCODE_URL=<URL_QRCODE_NFCE>
mvn -q exec:java
```

No PowerShell:

```powershell
$env:NFC_E_QRCODE_URL = "<URL_QRCODE_NFCE>"
mvn -q exec:java
```

## Exemplo de uso

```bash
mvn -q exec:java -Dexec.args="https://exemplo/qrcode.xhtml?p=31260404641376013467650700002314271351124718|2|1"
```

Saida esperada em JSON:

```json
{
  "url_qrcode": "https://exemplo/qrcode.xhtml?p=31260404641376013467650700002314271351124718|2|1",
  "chave_acesso": "31260404641376013467650700002314271351124718",
  "estabelecimento": {
    "nome": "EMPRESA X",
    "cnpj": "04641376013467",
    "inscricao_estadual": "0020488293197",
    "uf": "MG",
    "endereco_completo": "AV. FRANCISCO NEGRAO DE LIMA, 533, CEU AZUL, 3106200 - BELO HORIZONTE, MG",
    "logradouro": "AV. FRANCISCO NEGRAO DE LIMA",
    "numero": "533",
    "bairro": "CEU AZUL",
    "municipio": "BELO HORIZONTE",
    "cep": "3106200"
  },
  "consumidor": {
    "nome_razao_social": null,
    "cpf": "70193660660",
    "uf": null
  },
  "nota": {
    "destino_operacao": "1 - Operacao Interna",
    "consumidor_final": "1 - Sim",
    "presenca_comprador": "1 - Operacao presencial",
    "modelo": "65",
    "serie": "70",
    "numero": "231427",
    "data_emissao": "19/04/2026 10:57:21",
    "valor_total_servico": 42.21,
    "base_calculo_icms": 0.0,
    "valor_icms": 0.0,
    "protocolo": "131260697043739"
  },
  "totais": {
    "quantidade_total_itens": 2,
    "valor_total_nota": 42.21
  },
  "itens": [
    {
      "numero": 1,
      "descricao": "ITEM A",
      "codigo": "123",
      "quantidade": 1.0,
      "unidade": "UN",
      "valor_total": 6.48
    }
  ],
  "informacoes_complementares": "Texto complementar"
}
```

## Saida do programa

O programa imprime um JSON com os seguintes blocos:

- `url_qrcode`
- `chave_acesso`
- `estabelecimento`
- `consumidor`
- `nota`
- `totais`
- `itens`
- `informacoes_complementares`

Campos ausentes no HTML podem vir como `null` ou listas vazias, dependendo do que foi encontrado na pagina.

## Estrutura do projeto

```text
src/
  main/
    java/br/com/nuprecin/nfce/
      Main.java
      NfceScraper.java
      NfceParser.java
      NfceScraperException.java
      model/
      util/
  test/
    java/br/com/nuprecin/nfce/
```

### Principais classes

- `Main`: ponto de entrada da aplicacao.
- `NfceScraper`: faz a requisicao HTTP e encaminha o HTML para o parser.
- `NfceParser`: extrai os dados da pagina NFC-e.
- `ParsingUtils`: funcoes auxiliares de normalizacao e parsing.

## Testes

Execute a bateria de testes com:

```bash
mvn test
```

Os testes cobrem principalmente:

- extracao de chave de acesso pela URL;
- tolerancia a caracteres `|` na query string;
- parse completo de uma pagina HTML de NFC-e.

## Observacoes importantes

- A estrutura do HTML da NFC-e pode variar entre estados e emissores.
- O parser possui fallbacks para lidar com pequenas variacoes de markup.
- Caso a pagina retorne erro HTTP ou HTML vazio, a aplicacao encerra com excecao especifica.

## Contribuicao

Se quiser contribuir, sugiro o seguinte fluxo:

1. Crie uma branch para sua alteracao.
2. Adicione ou ajuste testes.
3. Rode `mvn test`.
4. Abra um pull request com a descricao da mudanca.

