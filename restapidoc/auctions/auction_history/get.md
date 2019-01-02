# Show Auction History

**URL** : `/auctions/$id/auction_history`

**Method** : `GET`

**Auth required** : NO

**Permissions required** : None

**Data constraints** : `{}`

## Success Responses

**Condition** : Auction exists but has no Bid registered yet.

**Code** : `200 OK`

**Content** : `{[]}`

### OR

**Condition** : There is at least one Bid for this Auction in the system.

**Code** : `200 OK`

**Content** : 

```json
[
    {
        "history": [
            {
                "bidderId": "2",
                "price": 105,
                "time": "2019-01-02 18:38:21"
            },
            {
                "bidderId": "1",
                "price": 100,
                "time": "2019-01-02 18:38:05"
            }
        ]
    }
]
```