# Show Auctions

**URL** : `/auctions/`

**Method** : `GET`

**Auth required** : NO

**Permissions required** : None

**Data constraints** : `{}`

## Success Responses

**Condition** : There is no Auction in the system yet.

**Code** : `200 OK`

**Content** : `{[]}`

### OR

**Condition** : There is at least one Auction in the system.

**Code** : `200 OK`

**Content** : In this example, the User can see two Auctions in that order:

```json
[
    {
        "auctions": [
            {
                "endDate": "2019-02-12 01:02:38",
                "floorPrice": 100,
                "incrementPolicy": 1,
                "itemId": "1",
                "startDate": "2019-01-10 01:00:58"
            },
            {
                "endDate": "2019-02-12 11:00:00",
                "floorPrice": 235,
                "incrementPolicy": 4,
                "itemId": "2",
                "startDate": "2019-01-13 12:12:12"
            }
        ]
    }
]
```
