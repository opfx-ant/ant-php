<?php
// PHPUnit_TextUI_ResultPrinter
//PHPUnit_Framework_TestResult
if (class_exists('PHPUnit_Util_Printer')) {
	class_alias('PHPUnit_TextUI_ResultPrinter', 'ResultPrinter');
	class_alias('PHPUnit_Framework_TestResult', 'TestResult');
	class_alias('PHPUnit_Util_Printer', 'Printer');
	class_alias('PHPUnit_Framework_TestListener', 'TestListener');
	class_alias('PHPUnit_Framework_Test', 'Test');
	class_alias('PHPUnit_Framework_TestSuite', 'TestSuite');
	class_alias('PHPUnit_Framework_TestCase', 'TestCase');
	class_alias('PHPUnit_Framework_AssertionFailedError', 'AssertionFailedError');
	class_alias('SebastianBergmann\CodeCoverage\CodeCoverage', 'CodeCoverage');
} else {
	class_alias('PHPUnit\Util\Printer', 'Printer');
	class_alias('PHPUnit\Framework\TestListener', 'TestListener');
	class_alias('PHPUnit\Framework\Test', 'Test');
	class_alias('PHPUnit\Framework\TestSuite', 'TestSuite');
	class_alias('PHPUnit\Framework\TestCase', 'TestCase');
	class_alias('PHPUnit\Framework\Warning', 'Warning');
	class_alias('PHPUnit\Framework\AssertionFailedError', 'AssertionFailedError');
	class_alias('PHPUnit\Framework\Exception', 'Exception2');
	class_alias('PHPUnit\Framework\ExpectationFailedException', 'ExpectationFailedException');
	class_alias('PHPUnit\Framework\Error\Warning', 'PHPUnit_Framework_Error_Warning');	
	class_alias('SebastianBergmann\CodeCoverage', 'CodeCoverage');
}

class PHPUnitResultPrinter@version.script@ extends ResultPrinter
{
	
	public function __construct($out = null, $verbose = false, $colors = self::COLOR_DEFAULT, $debug = false, $numberOfColumns = 80, $reverse = false)	{
		parent::__construct($out, $verbose, $colors, $debug, $numberOfColumns, $reverse);
	}
	
	public function printResult(TestResult $result) {
		
		
 		$cc=$result->getCodeCoverage();
 		if($cc!==null) {
			$s = serialize($cc);
 			file_put_contents('O:\work\opfx\proj\ant\master\ant-php\build\work\money\build\report\1.txt', $s);
 			$cc->setProcessUncoveredFilesFromWhitelist(false);
 			$cc->setAddUncoveredFilesFromWhitelist(false);
 			$cc->clear(); 			
 		}
 		parent::printResult($result);
	}
	
	protected function writeProgress($progress)
	{
		fwrite(STDOUT,$progress);
		$this->column++;
		$this->numTestsRun++;	
	}
	
	protected function printHeader()
	{
		$millies = round((microtime(true) - PHP_Timer::$requestTime)*1000);
		$mbytes =  memory_get_peak_usage(true) / 1048576;
		fwrite(STDOUT,"#T:$millies;M:$mbytes#");
	}
	
	protected function printDefects(array $defects, $type) {		
		if(empty($defects)) {
			return;
		}
		if($this->reverse){
			$defects = array_reverse($defects);
		}
		foreach($defects as $defect) {
			$trace = [];
			$e = $defect->thrownException();
			
			
			$trace[] = str_replace(["\r","\n"],['','#'],(string)$e);
			
			while ($e = $e->getPrevious()) {
				$trace[]=" Caused by " .str_replace(["\r","\n"],['','#'],(string)$e);
			}
			$trace = implode('#',$trace);
			$defect = sprintf("\n%s|%s#%s", $type,$defect->getTestName(),$trace);
			fwrite(STDERR,$defect);
		}
	}

	protected function printFooter(PHPUnit_Framework_TestResult $result)
	{
		if (count($result) === 0) {
			$this->writeWithColor(
					'fg-black, bg-yellow',
					'No tests executed!'
					);
			
			return;
		}
		
		if ($result->wasSuccessful() &&
				$result->allHarmless() &&
				$result->allCompletelyImplemented() &&
				$result->noneSkipped()) {
					$this->writeWithColor(
							'fg-black, bg-green',
							sprintf(
									'OK (%d test%s, %d assertion%s)',
									count($result),
									(count($result) == 1) ? '' : 's',
									$this->numAssertions,
									($this->numAssertions == 1) ? '' : 's'
									)
							);
				} else {
					if ($result->wasSuccessful()) {
						$color = 'fg-black, bg-yellow';
						
						if ($this->verbose) {
							$this->write("\n");
						}
						
						$this->writeWithColor(
								$color,
								'OK, but incomplete, skipped, or risky tests!'
								);
					} else {
						$this->write("\n");
						
// 						if ($result->errorCount()) {
// 							$color = 'fg-white, bg-red';
							
// 							$this->writeWithColor(
// 									$color,
// 									'ERRORS!'
// 									);
// 						} elseif ($result->failureCount()) {
// 							$color = 'fg-white, bg-red';
							
// 							$this->writeWithColor(
// 									$color,
// 									'FAILURES!'
// 									);
// 						} elseif ($result->warningCount()) {
// 							$color = 'fg-black, bg-yellow';
							
// 							$this->writeWithColor(
// 									$color,
// 									'WARNINGS!'
// 									);
// 						}
					}
					$count = count($result);
					fwrite(STDOUT,"T:$count;");
					$count = $this->numAssertions;
					fwrite(STDOUT,"A:$count;");
					$count = $result->errorCount();
					fwrite(STDOUT,"E:$count;");
					$count = $result->failureCount();
					fwrite(STDOUT,"F:$count;");
					$count = $result->warningCount();
					fwrite(STDOUT,"W:$count;");
					$count = $result->skippedCount();
					fwrite(STDOUT,"S:$count;");
					$count = $result->notImplementedCount();
					fwrite(STDOUT,"I:$count;");
					$count = $result->riskyCount();
					fwrite(STDOUT,"R:$count;");
					
				}
	}
	
}
