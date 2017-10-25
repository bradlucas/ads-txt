# ads-txt

A site which uses `ads-txt-crawler` to retrieve and store Ads.txt file contents



## REST API

### List Domains

    
```
$ curl -v --header "Accept:application/json" localhost:3000/api/domains
```

### Query Domain

By Id

```
$ curl -v --header "Accept:application/json" localhost:3000/api/domain/id/1
```

By name

```
$ curl -v --header "Accept:application/json" localhost:3000/api/domain/name/businessinsider.com
```

### List Records

```
$ curl -v --header "Accept:application/json" localhost:3000/api/records
```

### Query Records for Domain

By id

```
$ curl -v --header "Accept:application/json" localhost:3000/api/records/domain/id/1
```

By name

```
$ curl -v --header "Accept:application/json" localhost:3000/api/records/domain/name/businessinsider.com
```

### Check For Specific Record

Pass in domain-name, exchange-domain-name, seller-account-id and account-type. If a matching record is found a http status 200 is returned. If the record doesn't exist a 404 is returned.

This example is for a valid record.

```
$ curl -v --header "Accept:application/json" localhost:3000/api/check/businessinsider.com/google.com/pub-1037373295371110/DIRECT

< HTTP/1.1 200 OK

```

This example is for a does not exist.

```
$ curl -v --header "Accept:application/json" localhost:3000/api/check/businessinsider.com/google.com/pub-1037373295371110/RESELLER

< HTTP/1.1 404 Not Found
```


### Submit Domain

To submit a domain to be crawled you can post it as follows.

```
$ curl -d '{"name":"businessinsider.com"}' -H "Content-Type: application/json" -X POST localhost:3000/api/domain
```

        

  

## License

Copyright © 2017 Brad Lucas

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
