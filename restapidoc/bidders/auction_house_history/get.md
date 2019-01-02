# Get Bidder related Auction House History

**URL** : `/bidders/$id/auction_house_history`

**Method** : `GET`

**Auth required** : NO

**Permissions required** : None

**Data constraints** : `{}`

## Success Responses

**Condition** : There is no Auction in the system yet.

**Code** : `200 OK`

**Content** : `{[]}`

### OR

**Condition** : There is at least one Auction in the system and the Bidder has joined at least one.

**Code** : `200 OK`

**Content** : In this example, the User can see two Auctions in that order:

```json
[
    {
        "history": [
            {
                "status": {
                    "message": "not leading"
                },
                "auction": {
                    "endDate": "2019-01-02 18:34:00",
                    "floorPrice": 235,
                    "incrementPolicy": 4,
                    "itemId": "1",
                    "startDate": "2019-01-02 18:22:00"
                },
                "history": [
                    {
                        "bidderId": "2",
                        "price": 315,
                        "time": "2019-01-02 18:22:55"
                    },
                    {
                        "bidderId": "1",
                        "price": 300,
                        "time": "2019-01-02 18:22:05"
                    }
                ]
            },
            {
                "status": {
                    "message": "pending"
                },
                "auction": {
                    "endDate": "2019-01-02 18:34:00",
                    "floorPrice": 100,
                    "incrementPolicy": 1,
                    "itemId": "2",
                    "startDate": "2019-01-02 18:24:00"
                },
                "history": []
            }
        ]
    }
]
```

## Error Responses

**Condition** : No Bidder exists in the system for bidderId.

**Code** : `400 BAD REQUEST`

**Content example**

```json
{
    "description": "No action performed. Bidder 2 does not exists.",
    "success": false
}
```
