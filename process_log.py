import csv
import math
from optparse import OptionParser
from pprint import pprint
import scipy.interpolate
from matplotlib import pyplot as plt

parser = OptionParser()

options, args = parser.parse_args()
fname = args[0]

game_time = 9 * 60

tgf = 'TheGreaterFool'

resources = {
    '0': 'FlightA',
    '1': 'FlightD',
    '2': 'HotelTT',
    '3': 'HotelSS',
    '4': 'Alligator',
    '5': 'Amusement',
    '6': 'Museum'
}

bid_types = {
    's': 'submit',
    'r': 'replace',
    't': 'transact',
    'w': 'withdraw',
    'c': 'reject'
}

def _split(l, n):
    for i in xrange(0, len(l), n):
        yield l[i:i+n]

def load_info(fname):
    info = {
        'agents': {},
        'auctions': {}
    }

    def get_auction(auction_id):
        return info['auctions'][auction_to_name[auction_id]]

    def get_agent(agent_id):
        return info['agents'][agent_to_name[agent_id]]

    auction_to_name = {}
    agent_to_id = {'auction': 'auction'}
    agent_to_name = {'auction': 'auction'}

    def start_game(time, game_id, start_time, end_time, u_game_id=None,
                   game_type=None, num_agents=None):
        # start of game
        info['start'] = int(start_time)
        info['end'] = int(end_time)

    def version(time, server_version, server_name):
        info['version'] = server_version
        info['server'] = server_name

    def add_agent(time, agent_name, agent_id):
        info['agents'][agent_name] = {
            'clients': [],
            'alloc': []
        }
        agent_to_id[agent_name] = agent_id
        agent_to_name[agent_id] = agent_name

    def client_prefs(time, game_id, agent_id, *prefs):
        agent = get_agent(agent_id)
        for c in _split(prefs, 6):
            agent['clients'].append({
                'in': int(c[0]),
                'out': int(c[1]),
                'hotel': int(c[2]),
                'e1': int(c[3]),
                'e2': int(c[4]),
                'e3': int(c[5])
            })

    def start_auction(time, *auctions):
        for a in _split(auctions, 3):
            auction_id, resource, day = a
            resource = resources[resource]
            name = "%s%s" % (resource, day)
            auction_to_name[auction_id] = name
            info['auctions'][name] = {
                'resource': resource,
                'day': day,
                'sell': [],
                'buy': [],
                'transactions': [],
                'bids': {}
            }

    def transaction(time, buyer_id, seller_id, auction_id,
                    quantity, price, transaction_id=None):
        get_auction(auction_id)['transactions'].append({
            'time': time,
            'buyer': agent_to_name[buyer_id],
            'seller': agent_to_name[seller_id],
            'quantity': int(quantity),
            'price': float(price)
        })

    def quote(time, auction_id, sell_price, buy_price, *hqw):
        auction = get_auction(auction_id)
        auction['sell'].append({'time': time, 'price': float(sell_price)})
        auction['buy'].append({'time': time, 'price': float(buy_price)})

    def bid(time, bid_id, agent_id, auction_id, bid_type,
            process_status, *bids):
        auction = get_auction(auction_id)
        agent = agent_to_name[agent_id]
        bids = map(lambda b: tuple([int(b[0]), float(b[1])]), _split(bids, 2))

        if agent not in auction['bids']:
            auction['bids'][agent] = []
        auction['bids'][agent].append({
            'time': time,
            'type': bid_types[bid_type],
            'bids': bids
        })

    def close_auction(time, auction_id):
        get_auction(auction_id)['closed'] = time

    def end_game(time, game_id, u_game_id=None):
        pass

    def final_alloc(time, game_id, agent_id, *allocs):
        agent = get_agent(agent_id)
        for a in _split(allocs, 6):
            hotel = 'hotel TT' if a[2] else 'hotel SS'
            agent['alloc'].append({
                'in': int(a[0]),
                'out': int(a[1]),
                'hotel': hotel,
                'e1': int(a[3]),
                'e2': int(a[4]),
                'e3': int(a[5])
            })

    def final_score(time, game_id, agent_id, score, penalty, utility,
                    calc_time=None):
        agent = get_agent(agent_id)
        agent['results'] = {
            'score': float(score),
            'penalty': int(penalty),
            'utility': int(utility)
        }

    handlers = {
        'g': start_game,
        'v': version,
        'a': add_agent,
        'c': client_prefs,
        'u': start_auction,
        't': transaction,
        'q': quote,
        'b': bid,
        'z': close_auction,
        'x': end_game,
        'l': final_alloc,
        's': final_score
    }

    with open(fname) as f:
        reader = csv.reader(f)
        for r in reader:
            t = r[1]
            time = int(r[0]) - info.get('start', 0)
            args = r[2:]
            f = handlers[t]
            f(time, *args)

    return info


def interp_prices(prices, stop=game_time):
    values = [p['price'] for p in prices]
    times = [p['time'] for p in prices]

    # make sure nothing happens simultaneously
    i = 1
    for t1, t2 in zip(times[:-1], times[1:]):
        if t1 == t2:
            times[i] += 0.1
        i += 1

    if len(times) > 1:
        sci_interp = scipy.interpolate.interp1d(times, values, bounds_error=False, kind='nearest')
    else:
        sci_interp = lambda t: 0

    def f(t):
        if t > stop or len(times) == 0 or t < times[0]:
            return float('nan')
        elif t > times[-1]:
            return values[-1]
        else:
            return sci_interp(t)
    return [float(f(t)) for t in range(game_time)]

def bids_to_prices(bids):
    def bid_to_price(bid):
        if bid['type'] == 'withdraw' or len(bid['bids']) == 0:
            price = float('nan')
        else:
            price = max([b[1] for b in bid['bids']])

        return {
            'time': bid['time'],
            'price': price
        }
    bids = filter(lambda b: (b['type'] == 'submit'), bids)
    return map(bid_to_price, bids)


info = load_info(fname)
auctions = info['auctions']
agents = info['agents']
agent = agents[tgf]

def plot_auction(auction):
    plt.xlabel('Time')
    plt.ylabel('Price')
    plt.plot(interp_prices(auction['sell'], auction['closed']),
             color='b', label='Ask price', linewidth=2)
    plt.plot(interp_prices(auction['buy'], auction['closed']),
             color='g', label='Buy price', linewidth=2)
    for agent, bids in auction['bids'].iteritems():
        if agent == tgf:
            style = 'r-'
        else:
            style = '--'
        plt.plot(interp_prices(bids_to_prices(bids), auction['closed']),
                 style, label=agent)
    plt.legend(loc='lower right')
    plt.grid(True, 'both', 'y')
    plt.show()

if __name__ == '__main__':
    print 'Auction names are of the form "NameDay", where name is either: '
    print 'FlightA, FlightD, HotelTT, HotelSS, Alligator, Amusement, Museum'
    print 'and day is a number from 1 to 5.'
    while True:
        try:
            auction = auctions[raw_input('Enter an auction: ')]
        except KeyError:
            print 'Invalid auction name'
        else:
            plot_auction(auction)
