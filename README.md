[![License](https://img.shields.io/github/license/qmetry/qaf-support-elasticsearch.svg)](http://www.opensource.org/licenses/mit-license.php)
[![Release](https://img.shields.io/github/release/qmetry/qaf-support-elasticsearch.svg)](https://github.com/qmetry/qaf-support-elasticsearch/releases)
[![GitHub tag](https://img.shields.io/github/tag/qmetry/qaf-support-elasticsearch.svg)](https://github.com/qmetry/qaf-support-elasticsearch/tags)
[![StackExchange]( https://img.shields.io/badge/questions-Stack_Overflow-lightgray.svg?maxAge=2592000)](http://stackoverflow.com/questions/tagged/qaf)
[![Users-group]( https://img.shields.io/badge/users-Group-blue.svg?maxAge=2592000)](https://groups.google.com/forum/#!forum/qaf-users)
[![Help]( https://img.shields.io/badge/help-Documentation-blue.svg?maxAge=2592000)](https://qmetry.github.io/qaf/)

# Usage

1. Add [dependency](https://mvnrepository.com/artifact/com.qmetry/qaf-support-elasticsearch) to your project.
2. Provide elastic serach server url using `elasticsearch.host` property.
3. Import [dashboard](https://github.com/qmetry/qaf-support-elasticsearch/blob/master/src/com/qmetry/qaf/automation/elasticsearch/objects.ndjson) in kibana (first time only)

### Example properties:
```
elasticsearch.host=http://localhost:9200;http://localhost:9201
#elasticsearch.index is optional, default value is qaf_results
elasticsearch.index=MyPrjIndexName

```

NOTE: This library requires qaf version 3.0.0-RC3 or above.
