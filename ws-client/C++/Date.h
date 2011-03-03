#include <ctime>

class Date
{
public:
	Date();
	Date(const time_t& _time);
	
	~Date();
	time_t getUnixDate();
private:
	time_t unix_time;
};
