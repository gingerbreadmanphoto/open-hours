**Open hours** <br>
Application takes JSON-formatted opening hours of a restaurant as an input and outputs hours in more human readable format.
1) Run unit tests: `sbt test`
2) Run integration tests: `sbt it:test`
3) Run application: `docker-compose up` (run the published image) or `sbt run`
4) Request: `curl -X POST -d '{"monday": [{"type": "open", "value": 3600}, {"type": "closed", "value":7200}]}' 127.0.0.1:8090/schedule/pretty`

**Data format** <br>
Most restaurants work according to the fixed schedule that can vary for holidays.<br>
And most of the time there is the same schedule for weekdays and for weekends. <br>
Due to this fact there can be another option for the request representation.<br>
```
{
   "weekday": [
      { "type": "open", "value": 3600 },
      { "type": "closed", "value": 7200 },
   ],
   "weekend": [
      { "type": "open", "value": 3600 },
      { "type": "closed", "value": 7200 },
   ],
   "override": {
      "monday": [
          { "type": "open", "value": 0 },
          { "type": "closed", "value": 3600 },
      ]
   }
}
```

Also we can merge interval's edges to the single object:
```
{
   "weekday": [
      { "open": 3600, "closed": 7200 }
   ],
   "weekend": [
      { "open": 3600, "closed": 7200 }
   ],
   "override": {
      "monday": [
          // overnight on tuesday 8PM - 1AM
          { "open": 72000, "closed": 3600 }
      ]
   }
}
```
If we encounter the `closed` value less than the `open` one, we understand that there is an overnight schedule.<br>